ARG TERRAFORM_VERSION

FROM hashicorp/terraform:$TERRAFORM_VERSION

RUN apk update

RUN apk add yq python3 py-pip

RUN pip3 install google-api-python-client google-auth-httplib2 google-auth-oauthlib
