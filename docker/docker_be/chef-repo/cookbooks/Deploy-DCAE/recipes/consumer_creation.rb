sdc_be_vip = node['BE_VIP']

if node['disableHttp']
  protocol = "https"
  sdc_be_port = node['SDC']['BE'][:https_port]
else
  protocol = "http"
  sdc_be_port = node['SDC']['BE'][:http_port]
end

template "/var/tmp/consumers.py" do
  source "consumers.py.erb"
  owner "jetty"
  group "jetty"
  mode "0755"
  variables({
    :consumerName => node['DCAE']['consumerName'],
    :protocol => protocol,
    :catalog_ip => sdc_be_vip,
    :catalog_port => sdc_be_port
  })
end

bash "create dcaeTest user" do
  cwd '/var/tmp'
  code  <<-EOH
    python /var/tmp/consumers.py
  EOH
end
