#!/bin/bash

# Script to delete GitHub packages for a specific repository
# Usage: ./delete-github-package.sh <org_or_user> <repo_name> <token> [version] [package_type] [scope]
# Example: ./delete-github-package.sh storytellerF AFS ghp_... 1.0.0-SNAPSHOT maven user

set -u

if [ $# -lt 3 ]; then
    echo "Usage: $0 <org_or_user> <repo_name> <token> [version] [package_type] [scope]"
    echo "Example: $0 storytellerF AFS ghp_1234567890abcdef 1.0.0-SNAPSHOT maven user"
    exit 1
fi

ORG_OR_USER=$1
REPO_NAME=$2
TOKEN=$3
VERSION=${4:-all}
PACKAGE_TYPE=${5:-maven}
SCOPE=${6:-org}

echo "Targeting owner: $ORG_OR_USER"
echo "Targeting repository: $REPO_NAME"
echo "Package type: $PACKAGE_TYPE"
echo "Target version: $VERSION"

github_api_call() {
    local url=$1
    local method=${2:-GET}
    # Using a temporary file to separate body and status code more reliably
    local body_file=$(mktemp)
    local status_code=$(curl -s -L -o "$body_file" -w "%{http_code}" -X "$method" \
        -H "Authorization: token $TOKEN" \
        -H "Accept: application/vnd.github+json" \
        "$url")
    echo "$status_code"
    cat "$body_file"
    rm "$body_file"
}

# 1. Fetch Packages
if [ "$SCOPE" = "org" ]; then
    PACKAGES_URL="https://api.github.com/orgs/$ORG_OR_USER/packages?package_type=$PACKAGE_TYPE&per_page=100"
else
    PACKAGES_URL="https://api.github.com/users/$ORG_OR_USER/packages?package_type=$PACKAGE_TYPE&per_page=100"
fi

echo "Fetching packages list..."
RES=$(github_api_call "$PACKAGES_URL")
HTTP_CODE=$(echo "$RES" | head -n1)
RESPONSE=$(echo "$RES" | tail -n +2)

if [ "$HTTP_CODE" -eq 404 ] && [ "$SCOPE" = "user" ]; then
    echo "User packages not found at $PACKAGES_URL, trying /user/packages..."
    PACKAGES_URL="https://api.github.com/user/packages?package_type=$PACKAGE_TYPE&per_page=100"
    RES=$(github_api_call "$PACKAGES_URL")
    HTTP_CODE=$(echo "$RES" | head -n1)
    RESPONSE=$(echo "$RES" | tail -n +2)
fi

if [ "$HTTP_CODE" -ne 200 ]; then
    echo "Error: Failed to fetch packages (HTTP $HTTP_CODE)"
    echo "Response: $RESPONSE"
    exit 1
fi

# Filter packages belonging to the specified repository (case-insensitive)
PACKAGES=$(echo "$RESPONSE" | jq -r --arg repo "$REPO_NAME" '.[] | select(.repository.name != null and (.repository.name | ascii_downcase) == ($repo | ascii_downcase)) | .name' 2>/dev/null)

if [ -z "$PACKAGES" ]; then
    echo "No packages found matching repository '$REPO_NAME'."
    echo "Found packages for other repos:"
    echo "$RESPONSE" | jq -r '.[] | " - \(.name) (Repo: \(.repository.name // "N/A"))"' 2>/dev/null
    exit 0
fi

for PACKAGE in $PACKAGES; do
    echo "-------------------------------------------"
    echo "Processing package: $PACKAGE"

    # URL encode package name if necessary
    ENCODED_PACKAGE=$(echo "$PACKAGE" | sed 's/\./%2E/g')

    if [ "$VERSION" = "all" ]; then
        echo "  Mode: Delete entire package"
        if [ "$SCOPE" = "org" ]; then
            DELETE_URL="https://api.github.com/orgs/$ORG_OR_USER/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE"
        else
            DELETE_URL="https://api.github.com/user/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE"
        fi

        D_RES=$(github_api_call "$DELETE_URL" "DELETE")
        DEL_CODE=$(echo "$D_RES" | head -n1)
        if [ "$DEL_CODE" -eq 204 ]; then
            echo "    Successfully deleted package"
        else
            echo "    Failed to delete package (HTTP $DEL_CODE)"
            echo "    Response: $(echo "$D_RES" | tail -n +2)"
        fi
    else
        echo "  Mode: Delete specific version '$VERSION'"
        if [ "$SCOPE" = "org" ]; then
            VERSIONS_URL="https://api.github.com/orgs/$ORG_OR_USER/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE/versions?per_page=100"
        else
            VERSIONS_URL="https://api.github.com/users/$ORG_OR_USER/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE/versions?per_page=100"
        fi

        V_RES=$(github_api_call "$VERSIONS_URL")
        V_CODE=$(echo "$V_RES" | head -n1)
        V_RESPONSE=$(echo "$V_RES" | tail -n +2)

        if [ "$V_CODE" -eq 404 ] && [ "$SCOPE" = "user" ]; then
             echo "  Versions not found at $VERSIONS_URL, trying alternative..."
             VERSIONS_URL="https://api.github.com/user/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE/versions?per_page=100"
             V_RES=$(github_api_call "$VERSIONS_URL")
             V_CODE=$(echo "$V_RES" | head -n1)
             V_RESPONSE=$(echo "$V_RES" | tail -n +2)
        fi

        if [ "$V_CODE" -ne 200 ]; then
            echo "  Error fetching versions (HTTP $V_CODE): $(echo "$V_RESPONSE" | jq -r '.message // "Unknown error"' 2>/dev/null)"
            continue
        fi

        VERSION_IDS=$(echo "$V_RESPONSE" | jq -r --arg ver "$VERSION" '.[] | select(.name == $ver) | .id' 2>/dev/null)

        if [ -z "$VERSION_IDS" ]; then
            echo "  Version '$VERSION' not found for package $PACKAGE"
            echo "  Available versions: $(echo "$V_RESPONSE" | jq -r '[.[].name] | join(", ")' 2>/dev/null)"
            continue
        fi

        for VID in $VERSION_IDS; do
            echo "  Deleting version '$VERSION' (ID: $VID)..."
            if [ "$SCOPE" = "org" ]; then
                DELETE_VER_URL="https://api.github.com/orgs/$ORG_OR_USER/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE/versions/$VID"
            else
                DELETE_VER_URL="https://api.github.com/user/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE/versions/$VID"
            fi

            DV_RES=$(github_api_call "$DELETE_VER_URL" "DELETE")
            V_DEL_CODE=$(echo "$DV_RES" | head -n1)
            V_DEL_BODY=$(echo "$DV_RES" | tail -n +2)

            if [ "$V_DEL_CODE" -eq 204 ]; then
                echo "    Successfully deleted version"
            elif [ "$V_DEL_CODE" -eq 400 ] && echo "$V_DEL_BODY" | grep -q "last version"; then
                echo "    HTTP 400: Cannot delete last version. Deleting entire package instead..."
                if [ "$SCOPE" = "org" ]; then
                    DELETE_PKG_URL="https://api.github.com/orgs/$ORG_OR_USER/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE"
                else
                    DELETE_PKG_URL="https://api.github.com/user/packages/$PACKAGE_TYPE/$ENCODED_PACKAGE"
                fi
                DP_RES=$(github_api_call "$DELETE_PKG_URL" "DELETE")
                DP_CODE=$(echo "$DP_RES" | head -n1)
                if [ "$DP_CODE" -eq 204 ]; then
                    echo "    Successfully deleted entire package"
                else
                    echo "    Failed to delete package (HTTP $DP_CODE)"
                fi
            else
                echo "    Failed to delete version (HTTP $V_DEL_CODE)"
                echo "    Response: $V_DEL_BODY"
            fi
        done
    fi
done

echo "Done."
