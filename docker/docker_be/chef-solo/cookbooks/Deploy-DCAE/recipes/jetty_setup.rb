jetty_base = "#{node['JETTY_BASE']}"
jetty_home = "#{node['JETTY_HOME']}"

#Set the http module option
if node['disableHttp']
  http_option = "#--module=http"
else
  http_option = "--module=http"
end


printf("DEBUG: [%s]:[%s] disableHttp=[%s], http_option=[%s] !!! \n", cookbook_name, recipe_name, node['disableHttp'], http_option )


directory "Jetty_etcdir_creation" do
    path "/#{jetty_base}/etc"
    owner 'jetty'
    group 'jetty'
    mode '0755'
    action :create
end


# Create Keystore
cookbook_file "/#{jetty_base}/etc/keystore" do
   source "keystore"
   owner "jetty"
   group "jetty"
   mode 0755
end

# Create Truststore
cookbook_file "/#{jetty_base}/etc/truststore" do
   source "truststore"
   owner "jetty"
   group "jetty"
   mode 0755
end

bash "create-jetty-modules" do
  cwd "#{jetty_base}"
  code <<-EOH
    cd "#{jetty_base}"
    java -jar "/#{jetty_home}"/start.jar --add-to-start=deploy
    java -jar "/#{jetty_home}"/start.jar --add-to-startd=http,https,logging,setuid
  EOH
end

# configure Jetty modules
template "http-ini" do
   path "/#{jetty_base}/start.d/http.ini"
   source "http-ini.erb"
   owner "jetty"
   group "jetty"
   mode "0755"
   variables ({
     :http_option => http_option ,
     :http_port => "#{node['DCAE']['BE'][:http_port]}"
    })
end

template "https-ini" do
   path "/#{jetty_base}/start.d/https.ini"
   source "https-ini.erb"
   owner "jetty"
   group "jetty"
   mode "0755"
   variables ({
     :https_port => "#{node['DCAE']['BE'][:https_port]}"
   })
end

template "ssl-ini" do
   path "/#{jetty_base}/start.d/ssl.ini"
   source "ssl-ini.erb"
   owner "jetty"
   group "jetty"
   mode "0755"
   variables ({ 
     :https_port => "#{node['DCAE']['BE'][:https_port]}" ,
     :jetty_keystore_pwd => "#{node['jetty'][:keystore_pwd]}" ,
     :jetty_keymanager_pwd => "#{node['jetty'][:keymanager_pwd]}" ,
     :jetty_truststore_pwd => "#{node['jetty'][:truststore_pwd]}"
   })
end

