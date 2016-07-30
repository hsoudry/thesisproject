<?php
  if(!function_exists('buildQuery1')) {

    function buildQuery1($query_time, $path, $user_name) {
      $client = AWS::createClient('EMR');

      $result = $client->runJobFlow([
	'ReleaseLabel' => 'emr-4.7.2',
        'Applications' => [
          [
            'Name' => 'Hadoop',
          ],
        ],
	'Instances' => [
          'Ec2KeyName' => 'EMR key',
          'InstanceGroups' => [
            [
              'Configurations' => [
                [
                  'Classification' => 'hadoop-env',
                  'Configurations' => [
                    [
		      'Classification' => 'export',
                      'Configurations' => [],
                      'Properties' => ['JAVA_HOME' => '/usr/lib/jvm/java-1.8.0'],
                    ],
		  ],
                  'Properties' => [],
                ],
              ],
              'InstanceCount' => 1,
              'InstanceRole' => 'MASTER',
              'InstanceType' => 'm3.xlarge',
              'Name' => 'Master instance group - 1',
            ],
            [
              'Configurations' => [
                [
                  'Classification' => 'hadoop-env',
                  'Configurations' => [
		    [
                      'Classification' => 'export',
                      'Configurations' => [],
                      'Properties' => ['JAVA_HOME' => '/usr/lib/jvm/java-1.8.0'],
                    ],
		  ],
                  'Properties' => [],
                ],
              ],
              'InstanceCount' => 2,
              'InstanceRole' => 'CORE',
              'InstanceType' => 'm3.xlarge',
              'Name' => 'Core instance group - 2',
            ],
          ],
        ],
        'Name' => 'Query 1 cluster',
	'JobFlowRole' => 'EMR_EC2_DefaultRole',
	'ServiceRole' => 'EMR_DefaultRole',
	'LogUri' => 's3://thesisdata/logs/',
        'Steps' => [
          [
            'ActionOnFailure' => 'TERMINATE_CLUSTER',
            'HadoopJarStep' => [
              'Args' => ['s3-dist-cp','--s3Endpoint=s3.amazonaws.com','--src=s3://thesisdata/input/','--dest=hdfs:///input'],
              'Jar' => "command-runner.jar",
            ],
            'Name' => 'Copy input from S3 to cluster',
          ],
          [
            'ActionOnFailure' => 'TERMINATE_CLUSTER',
            'HadoopJarStep' => [
              'Args' => ['hdfs:///input', 'hdfs:///output/'.$user_name."/".$path, $query_time],
              'Jar' => "s3://thesisdata/jar/TopTenRoutes.jar",
            ],
            'Name' => 'Top Ten Routes computation',
          ],
          [
            'ActionOnFailure' => 'TERMINATE_CLUSTER',
            'HadoopJarStep' => [
              'Args' => ['s3-dist-cp','--s3Endpoint=s3.amazonaws.com','--src=hdfs:///output/'.$user_name."/".$path,'--dest=hdfs:///input'],
              'Jar' => "command-runner.jar",
            ],
            'Name' => 'Copy output from cluster to S3',
          ],
        ],
      ]);
      return $result;
    }
  }
  if(!function_exists('buildQuery2')) {

    function buildQuery2($query_time, $path, $user_name) {
      $client = AWS::createClient('EMR');

      $result = $client->runJobFlow([
	'ReleaseLabel' => 'emr-4.7.2',
        'Applications' => [
          [
            'Name' => 'Hadoop',
          ],
        ],
	'Instances' => [
          'Ec2KeyName' => 'EMR key',
          'InstanceGroups' => [
            [
              'Configurations' => [
                [
                  'Classification' => 'hadoop-env',
                  'Configurations' => [
                    [
		      'Classification' => 'export',
                      'Configurations' => [],
                      'Properties' => ['JAVA_HOME' => '/usr/lib/jvm/java-1.8.0'],
                    ],
		  ],
                  'Properties' => [],
                ],
              ],
              'InstanceCount' => 1,
              'InstanceRole' => 'MASTER',
              'InstanceType' => 'm3.xlarge',
              'Name' => 'Master instance group - 1',
            ],
            [
              'Configurations' => [
                [
                  'Classification' => 'hadoop-env',
                  'Configurations' => [
		    [
                      'Classification' => 'export',
                      'Configurations' => [],
                      'Properties' => ['JAVA_HOME' => '/usr/lib/jvm/java-1.8.0'],
                    ],
		  ],
                  'Properties' => [],
                ],
              ],
              'InstanceCount' => 2,
              'InstanceRole' => 'CORE',
              'InstanceType' => 'm3.xlarge',
              'Name' => 'Core instance group - 2',
            ],
          ],
        ],
        'Name' => 'Query 2 cluster',
	'JobFlowRole' => 'EMR_EC2_DefaultRole',
	'ServiceRole' => 'EMR_DefaultRole',
	'LogUri' => 's3://thesisdata/logs/',
        'Steps' => [
          [
            'ActionOnFailure' => 'TERMINATE_CLUSTER',
            'HadoopJarStep' => [
              'Args' => ['s3-dist-cp','--s3Endpoint=s3.amazonaws.com','--src=s3://thesisdata/input/','--dest=hdfs:///input'],
              'Jar' => "command-runner.jar",
            ],
            'Name' => 'Copy input from S3 to cluster',
          ],
          [
            'ActionOnFailure' => 'TERMINATE_CLUSTER',
            'HadoopJarStep' => [
              'Args' => ['hdfs:///input', 'hdfs:///output/'.$user_name."/".$path, $query_time],
              'Jar' => "s3://thesisdata/jar/TopTenProfitability.jar",
            ],
            'Name' => 'Top Ten Most Profitable areas computation',
          ],
          [
            'ActionOnFailure' => 'TERMINATE_CLUSTER',
            'HadoopJarStep' => [
              'Args' => ['s3-dist-cp','--s3Endpoint=s3.amazonaws.com','--src=hdfs:///output/'.$user_name."/".$path,'--dest=hdfs:///input'],
              'Jar' => "command-runner.jar",
            ],
            'Name' => 'Copy output from cluster to S3',
          ],
        ],
      ]);
      return $result;
    }
  }
 ?>
