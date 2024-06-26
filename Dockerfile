FROM alpine:3.20

WORKDIR /workdir

COPY Gemfile* ./

RUN apk add --no-cache bash ca-certificates openjdk11 ruby ruby-etc ruby-json ruby-dev build-base && \
    gem install bundler && \
    bundler install && \
    adduser --uid 2004 --disabled-password --gecos "" docker
COPY docs /docs

COPY /target/universal/stage/ /workdir/
RUN chmod +x /workdir/bin/codacy-metrics-rubocop
USER docker
ENTRYPOINT [ "bin/codacy-metrics-rubocop" ]
