FROM nginx:1.27-alpine

COPY nginx.conf.template /etc/nginx/templates/default.conf.template

CMD ["/bin/sh", "-c", "envsubst '$PORT $AUTH_SERVICE_ORIGIN $PROFILE_SERVICE_ORIGIN $CHAT_SERVICE_ORIGIN $CALL_SERVICE_ORIGIN' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
