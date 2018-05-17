default['JETTY_BASE'] = "/var/lib/jetty"
default['JETTY_HOME'] = "/usr/local/jetty"
default['APP_LOG_DIR'] = "/opt/logs/be"

default['DCAE']['consumerName'] = "dcaeDesigner"
default['DCAE']['consumerPass'] = "Aa123456"

default['DCAE']['BE'][:http_port] = 8082
default['DCAE']['BE'][:https_port] = 8444

default['SDC']['BE'][:http_port] = 8080
default['SDC']['BE'][:https_port] = 8443

default['jetty'][:keystore_pwd] = "OBF:1cp61iuj194s194u194w194y1is31cok"
default['jetty'][:keymanager_pwd] = "OBF:1cp61iuj194s194u194w194y1is31cok"
default['jetty'][:truststore_pwd] = "OBF:1cp61iuj194s194u194w194y1is31cok"

default['disableHttp'] = true

