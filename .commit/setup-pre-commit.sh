# 将pre-commit.template 作为git hooks 的文件
# 实际执行的是pre-commit.sh，可以随意修改此文件而不必重复设置
destination_dir=".git/hooks"
# 生成时间戳，格式为YYYYMMDD-HHMMSS
timestamp=$(date +%Y%m%d-%H%M%S)

# 构造含有时间戳的目标文件名
destination_file="${destination_dir}/pre-commit.$timestamp"

target_file="${destination_dir}/pre-commit"

[ -f "$target_file" ] && cp "$target_file" "$destination_file"
cp .commit/pre-commit.template "$target_file"
if [[ "$(uname)" == "Darwin" ]]; then
    xattr -d com.apple.provenance "$target_file"
fi

chmod +x "$target_file"