How to run:
mvn exec:java -Dexec.mainClass=Main -Dexec.args="'environment.json' 'config.json'"

environment.json example:

{
    "dcaeBeHost": "http://135.91.225.81",
    "dcaeBePort": "8080",
    "apiPath": "/dcae"
    "userEditor": "admin"
}

config.json example:
   {
   	"templateInfo": [{
   		"name": "SNMP Fault",
   		"description": "SNMP FM with Map-Supplement-Enrich",
   		"category": "Template / Base Monitoring Template",
   		"subCategory":"some subCategory",
   		"updateIfExist": "true",
   		"composition": [{
   			"type": "Map",
   			"alias": "mapper"
   		}, {
   			"type": "Supplement",
   			"alias": "sup"
   		}, {
   			"type": "Enrich",
   			"alias": "enrich"
   		}]
   	},
   		{
   			"name": "FOI",
   			"description": "FOI SFTP with FOI-Collector and Docker-Map",
   			"category": "Template / Base Monitoring Template",
   			"subCategory":"some subCategory",
   			"updateIfExist": "true",
   			"composition": [{
   				"type": "FOI Collector",
   				"alias": "collector"
   			}, {
   				"type": "DockerMap",
   				"alias": "map"
   			}],
   			"relation": [{
   				"fromComponent": "collector.FOISftp",
   				"fromRequirement": "stream_publish_0",
   				"toComponent": "map.topic1",
   				"toCapability": "topic"
   			}]
   		},
   		{
   			"name": "Syslog non-VES Collector",
   			"description": "Syslog flow with Syslog Collector",
   			"category": "Template / Base Monitoring Template",
   			"subCategory":"some subCategory",
   			"updateIfExist": "true",
   			"composition": [{
   				"type": "Syslog",
   				"alias": "collector"
   			}]
   		}
   	]
   }