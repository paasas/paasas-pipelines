FROM alpine

ARG VERSION

ADD paasas-pipelines-release/concourse-pipelines /usr/bin/pipelines-concourse

RUN chmod +x /usr/bin/pipelines-concourse

ENTRYPOINT pipelines-concourse