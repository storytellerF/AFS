FROM storytellerf/android-in-docker:latest-dev

ARG USER_NAME

USER root

# 如果需要在容器中访问docker 的话
RUN groupadd -g 1001 docker \
    && usermod -aG docker $USER_NAME

USER $USER_NAME
WORKDIR /home/$USER_NAME

RUN SNIPPET="export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
    && echo "$SNIPPET" >> ~/.bashrc