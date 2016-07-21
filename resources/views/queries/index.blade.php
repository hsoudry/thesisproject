@extends('layouts.app')

@section('content')
@if(session('status'))
<div class="col-sm-offset-1 col-xs-12 col-sm-10 alert alert-success">
    <p>
      @if(session('status')=='CREATED')
        New query successfully created !
      @else
        Query successfully updated !
      @endif
    </p>
</div>
@endif
<div class="row">
  <div class="col-sm-offset-2 col-xs-12 col-sm-8">
         <div class="panel panel-default">
          <div class="panel-heading">
            <h4>Your Queries</h4>
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
                <td>{{$query->created_at}}</td>
                <td>
                  @if($query->completion_time=='')
                    Not yet completed
                  @else
                    {{$query->completion_time}}
                  @endif
                </td>
                <td><a href={{$query->path}}>{{$query->path}}</a></td>
              </tr>
            @endforeach
          </tbody>
          </table>
        </div>
        <a href="queries/create"><button type="button" name="newQuery" class="btn btn-primary form-control">Create a new query</button></a>
      </div>
    </div>
@stop
