default['JETTY_BASE'] = "/var/lib/jetty"
default['JETTY_HOME'] = "/usr/local/jetty"
default['APP_LOG_DIR'] = "/opt/logs/be"

default['DCAE']['consumerName'] = "dcaeDesigner"
default['DCAE']['consumerPass'] = "Aa123456"

default['DCAE']['BE']['http_port'] = 8082
default['DCAE']['BE']['https_port'] = 8444
default['DCAE']['TOSCA_LAB']['http_port'] = 8085

default['SDC']['BE']['http_port'] = 8080
default['SDC']['BE']['https_port'] = 8443
default['DCAE']['TOSCA_LAB']['https_port'] = 8085
# TO CHANGE THE TRUSTSTORE CERT THE JVM CONFIGURATION
# MUST BE ALSO CHANGE IN THE startup.sh FILE
default['jetty']['keystore_pwd'] = "rTIS;B4kM]2GHcNK2c3B4&Ng"
default['jetty']['keymanager_pwd'] = "rTIS;B4kM]2GHcNK2c3B4&Ng"
default['jetty']['truststore_pwd'] = "Y,f975ZNJfVZhV*{+Y[}pA?0"

default['disableHttp'] = true

default['DCAE_TOSCA_LAB_VIP'] = "localhost"

