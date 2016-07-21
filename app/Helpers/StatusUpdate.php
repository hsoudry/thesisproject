<?php

if(!function_exists('updateQueryStatus')) {
  function updateQueryStatus($queries) {
    $client = AWS::createClient('EMR');
    $clusters = json_decode($client->listClusters([]));

    foreach ($queries as $query) {
      $found = false;
      foreach ($clusters->clusters as $cluster) {
        if($cluster->Id == $query->job_flow_id && $query->completion_time == NULL) {
          $query->status = $cluster->Status->State;
          $query->completion_time = $cluster->Status->Timeline->EndDateTime;
          $found = true;
        }
      }
      if($found == false) {
        $query->status = "COMPLETED";
      }
      $query->save();
    }
  }
}

?>
