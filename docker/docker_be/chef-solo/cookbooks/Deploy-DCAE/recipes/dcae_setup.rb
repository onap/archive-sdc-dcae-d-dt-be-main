jetty_base = "#{node['JETTY_BASE']}"
dcae_logs = "#{node['APP_LOG_DIR']}"

sdc_be_vip = node['BE_VIP']
dcae_tosca_lab_vip = node['DCAE_TOSCA_LAB_VIP']


if node['disableHttp']
  protocol = "https"
  sdc_be_port = node['SDC']['BE']['https_port']
  dcae_be_port = node['DCAE']['BE']['https_port']
  dcae_tosca_lab_port = node['DCAE']['TOSCA_LAB']['https_port']
else
  protocol = "http"
  sdc_be_port = node['SDC']['BE'][:http_port]
  dcae_be_port = node['DCAE']['BE'][:http_port]
  dcae_tosca_lab_port = node['DCAE']['TOSCA_LAB']['http_port']
end

printf("DEBUG: [%s]:[%s] disableHttp=[%s], protocol=[%s], sdc_be_vip=[%s], sdc_be_port=[%s] !!! \n", cookbook_name, recipe_name, node['disableHttp'], protocol, sdc_be_vip ,sdc_be_port )

raise "[ERROR] 'SDC_BE_FQDN' is not defined" if sdc_be_vip.nil? || sdc_be_vip == ""

directory "#{jetty_base}/config" do
  owner "jetty"
  group "jetty"
  mode '0755'
  recursive true
  action :create
end

directory "#{jetty_base}/config/dcae-be" do
  owner "jetty"
  group "jetty"
  mode '0755'
  recursive true
  action :create
end

directory "#{jetty_base}/config/dcae-be/ves-schema" do
  owner "jetty"
  group "jetty"
  mode '0755'
  recursive true
  action :create
end

template "dcae-be-config" do
  sensitive true
  path "#{jetty_base}/config/dcae-be/application.properties"
  source "dcae-application.properties.erb"
  owner "jetty"
  group "jetty"
  mode "0755"
  variables ({
    :consumerAuth => node['DCAE']['consumerName'] + ":" + node['DCAE']['consumerPass'] ,
    :dcae_be_port => dcae_be_port,
    :sdc_be_vip => sdc_be_vip,
    :sdc_be_port => sdc_be_port,
    :protocol => protocol,
    :dcae_tosca_lab_vip => dcae_tosca_lab_vip,
    :dcae_tosca_lab_port => dcae_tosca_lab_port
  })
end

cookbook_file "#{jetty_base}/config/dcae-be/ves-schema/CommonEventFormat_v4.1.json" do
  source "CommonEventFormat_v4.1.json"
  owner "jetty"
  group "jetty"
  mode 0755
end

cookbook_file "#{jetty_base}/config/dcae-be/ves-schema/CommonEventFormat_v5.3.json" do
  source "CommonEventFormat_v5.3.json"
  owner "jetty"
  group "jetty"
  mode 0755
end

template "dcae-error-configuration" do
  sensitive true
  path "#{jetty_base}/config/dcae-be/error-configuration.yaml"
  source "dcae-error-configuration.erb"
  owner "jetty"
  group "jetty"
  mode "0755"
end

template "dcae-logback-spring-config" do
  sensitive true
  path "#{jetty_base}/config/dcae-be/logback-spring.xml"
  source "dcae-logback-spring.erb"
  owner "jetty"
  group "jetty"
  mode "0755"
end

directory "#{dcae_logs}" do
  owner "jetty"
  group "jetty"
  mode '0755'
  recursive true
  action :create
end