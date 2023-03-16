from node as compiler
#WORKDIR /expoll/lib
#COPY ./lib .
#RUN npm install
#RUN npm run build

WORKDIR /expoll/frontend_server
COPY ./client .

RUN npm install --legacy-peer-deps
RUN npm run build

From alpine:latest
WORKDIR /www/data
# Install nginx and php
RUN apk update && apk upgrade
RUN apk add --no-cache \
  curl \
  caddy \
  php81 \
  php81-ctype \
  php81-curl \
  php81-dom \
  php81-fpm \
  php81-gd \
  php81-intl \
  php81-mbstring \
  php81-mysqli \
  php81-opcache \
  php81-openssl \
  php81-phar \
  php81-session \
  php81-xml \
  php81-xmlreader \
  php81-zlib \
  supervisor
# Create symlink so programs depending on `php` still function
RUN rm /usr/bin/php
RUN ln -s /usr/bin/php81 /usr/bin/php

# Copy additional files
COPY ./server/caddy/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
# configure php fpm
COPY ./server/caddy/fpm-pool.conf /etc/php81/php-fpm.d/www.conf
COPY ./server/caddy/php.ini /etc/php81/conf.d/custom.ini

# copy caddy config
COPY ./server/caddy/Caddyfile /etc/caddy/Caddyfile

# Make sure files/folders needed by the processes are accessible when they run under the nobody user
RUN chown -R nobody.nobody /www/data /run /etc/caddy

# Switch to use a non-root user from here on
USER nobody

COPY --from=0 --chown=nobody /expoll/frontend_server/dist /www/data

EXPOSE 80
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]

HEALTHCHECK --timeout=30s CMD curl --silent --fail http://localhost:80/