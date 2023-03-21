ARG TERRAFORM_VERSION

FROM hashicorp/terraform:$TERRAFORM_VERSION

RUN apk update

RUN apk add yq

RUN apk add python3

RUN python pip install google.auth && \
  python pip install google.oauth2 && \
  python pip install googleapiclient && \
  python pip install absl
