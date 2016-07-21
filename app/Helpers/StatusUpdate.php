<?php

if(!function_exists('updateQueryStatus')) {
  function updateQueryStatus($queries) {
    $client = AWS::createClient('EMR');
    $result = $client->listClusters([]);

    foreach ($queries as $query) {
      $found = false;
      foreach ($result['Clusters'] as $cluster) {
        if($cluster['Id'] == $query->job_flow_id && $query->completion_time == NULL) {
          $job_status = $cluster['Status']['State'];
          if(($job_status == 'TERMINATED')||($job_status == 'TERMINATED_WITH_ERRORS')) {
            $query->status = $job_status;
            $query->completion_time = $cluster['Status']['Timeline']['EndDateTime'];
          }
          $found = true;
        }
      }
      if($found == false) {
        $query->status = "TERMINATED";
      }
      $query->save();
    }
  }
}

?>
