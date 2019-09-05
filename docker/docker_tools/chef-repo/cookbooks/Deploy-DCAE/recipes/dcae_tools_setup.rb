dcae_be_host = node['DCAE_BE_VIP']

if node['disableHttp']
  protocol = "https"
  dcae_be_port = node['DCAE']['BE'][:https_port]
else
  protocol = "http"
  dcae_be_port = node['DCAE']['BE'][:http_port]
end

printf("DEBUG: [%s]:[%s] disableHttp=[%s], protocol=[%s], dcae_be_vip=[%s], dcae_be_port=[%s] !!! \n", cookbook_name, recipe_name, node['disableHttp'], protocol, dcae_be_host ,dcae_be_port )

directory "Jetty_etc dir_creation" do
  path "#{ENV['JETTY_BASE']}/etc"
  owner 'jetty'
  group 'jetty'
  mode '0755'
  action :create
end


cookbook_file "#{ENV['JETTY_BASE']}/etc/org.onap.sdc.trust.jks" do
  source "org.onap.sdc.trust.jks"
  owner "jetty"
  group "jetty"
  mode 0755
end


directory "#{ENV['JETTY_BASE']}/conf" do
  mode '0755'
  owner "jetty"
  group "jetty"
  recursive true
  action :create
end


template "dcae-tools-config-yaml" do
  sensitive true
  path "/#{ENV['JETTY_BASE']}/conf/environment.json"
  source "environment.json.erb"
  mode "0755"
  owner "jetty"
  group "jetty"
  variables({
    :dcae_be_host => dcae_be_host,
    :dcae_be_port => dcae_be_port,
    :protocol => protocol
  })
end


cookbook_file "/#{ENV['JETTY_BASE']}/conf/config.json" do
  sensitive true
  source "config.json"
  owner "jetty"
  group "jetty"
  mode "0755"
  action :create
end
