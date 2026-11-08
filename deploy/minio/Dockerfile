FROM golang:1.24.6-alpine3.21 AS build
ARG MINIO_VERSION=RELEASE.2025-10-15T17-29-55Z
ENV GOMAXPROCS=2 GOFLAGS=-p=2
RUN apk add --no-cache git
RUN CGO_ENABLED=0 go install github.com/minio/minio@${MINIO_VERSION}
RUN CGO_ENABLED=0 go install github.com/minio/mc@RELEASE.2025-08-13T08-35-41Z

FROM alpine:3.21
RUN apk add --no-cache ca-certificates curl \
    && adduser -S -u 10001 -h /data minio
COPY --from=build /go/bin/minio /usr/local/bin/minio
COPY --from=build /go/bin/mc /usr/local/bin/mc
COPY product-images-policy.json /etc/minio/product-images-policy.json
USER 10001:10001
EXPOSE 9000 9001
ENTRYPOINT ["/usr/local/bin/minio"]
CMD ["server", "/data", "--console-address", ":9001"]
