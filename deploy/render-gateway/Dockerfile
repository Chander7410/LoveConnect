FROM nginx:1.27-alpine

COPY nginx.conf.template /etc/nginx/templates/default.conf.template

CMD ["/bin/sh", "-c", "envsubst '$PORT $AUTH_SERVICE_HOSTPORT $PROFILE_SERVICE_HOSTPORT $CHAT_SERVICE_HOSTPORT $CALL_SERVICE_HOSTPORT' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
