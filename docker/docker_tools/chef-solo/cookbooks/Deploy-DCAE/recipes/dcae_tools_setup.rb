workspace_dir = "#{node['WORKSPACE_DIR']}"

dcae_be_host = node['DCAE_BE_VIP']

if node['disableHttp']
  protocol = "https"
  dcae_be_port = node['DCAE']['BE'][:https_port]
else
  protocol = "http"
  dcae_be_port = node['DCAE']['BE'][:http_port]
end

printf("DEBUG: [%s]:[%s] disableHttp=[%s], protocol=[%s], dcae_be_vip=[%s], dcae_be_port=[%s] !!! \n", cookbook_name, recipe_name, node['disableHttp'], protocol, dcae_be_host ,dcae_be_port )


directory "#{workspace_dir}/conf" do
  mode '0755'
  owner "dcae"
  group "dcae"
  recursive true
  action :create
end


template "dcae-tools-config-yaml" do
  sensitive true
  path "/#{workspace_dir}/conf/environment.json"
  source "environment.json.erb"
  mode "0755"
  owner "dcae"
  group "dcae"
  variables ({
    :dcae_be_host => dcae_be_host,
    :dcae_be_port => dcae_be_port,
    :protocol => protocol
  })
end


cookbook_file "/#{workspace_dir}/conf/config.json" do
  sensitive true
  source "config.json"
  owner "dcae"
  group "dcae"
  mode "0755"
  action :create
end
