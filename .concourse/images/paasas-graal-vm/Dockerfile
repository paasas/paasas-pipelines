FROM kubile/ubuntu-sdkman:23.04

RUN apt-get update

RUN apt-get install build-essential zlib1g-dev -y

RUN bash -c ". /root/.sdkman/bin/sdkman-init.sh && sdk install java 22.3.r17-nik sdk use java 22.3.r17-ni"