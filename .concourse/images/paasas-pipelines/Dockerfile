FROM alpine

ARG VERSION

ADD https://github.com/paasas/paasas-pipelines/releases/download/$VERSION/pipelines-concourse /usr/bin/pipelines-concourse

RUN chmod +x /usr/bin/pipelines-concourse

ENTRYPOINT pipelines-concourse