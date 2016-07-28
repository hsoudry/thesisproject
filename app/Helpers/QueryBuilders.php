<?php
  if(!function_exists('buildQuery1')) {

    function buildQuery1($query_time, $path) {
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
            'ActionOnFailure' => 'CONTINUE',
            'HadoopJarStep' => [
              'Args' => ['s3://thesisdata/input', 's3://thesisdata/output/'.$path, $query_time],
              'Jar' => "s3://thesisdata/jar/TopTenRoutes.jar",
            ],
            'Name' => 'Top Ten Routes computation',
          ],
        ],
      ]);
      return $result;
    }
  }
  if(!function_exists('buildQuery2')) {

    function buildQuery2($query_time, $path) {
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
            'ActionOnFailure' => 'CONTINUE',
            'HadoopJarStep' => [
              'Args' => ['s3://thesisdata/input', 's3://thesisdata/output/'.$path, $query_time],
              'Jar' => "s3://thesisdata/jar/TopTenProfitability.jar",
            ],
            'Name' => 'Top Ten Routes computation',
          ],
        ],
      ]);
      return $result;
    }
  }
 ?>
