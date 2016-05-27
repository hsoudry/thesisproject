@extends('layouts.app')

@section('content')
<div class="row">
  <div class="col-sm-1"></div>
  <div class="col-xs-12 col-sm-10">
         <div class="panel panel-default">
          <div class="panel-heading">
            <h5>Your Queries</h5>
            </div>
            <table class="table table-striped">
              <thead>
                <tr>
                  <th>Query type</th>
                  <th>Status</th>
                  <th>Request time</th>
                  <th>Completion time</th>
                  <th>Results</th>
                </tr>
              </thead>
              <tbody>
              @foreach($queries as $query)
              <tr>
                <td>{{$query->query_type}}</td>
                <td>{{$query->status}}</td>
                <td>{{$query->request_time}}</td>
                <td>{{$query->completion_time}}</td>
                <td>{{$query->file_link}}</td>
              </tr>
            @endforeach
          </tbody>
          </table>
        </div>
</div>
</div>
@stop
