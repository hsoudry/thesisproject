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
<div class="container">
  <div class="row">
    <div class="col-xs-12">
           <div class="panel panel-default">
            <div class="panel-heading">
              <h4>Your Queries</h4>
              </div>
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>Query type</th>
                    <th>Cluster status</th>
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
                      @if(($query->status == 'TERMINATED' || ($query->status == 'TERMINATED_WITH_ERRORS')))
                        NOT AVAILABLE
                      @else

                      @endif
                    @else
                      {!!Carbon\Carbon::parse($query->completion_time)->format('Y-m-d H:i:s')!!}
                    @endif
                  </td>
                  <td>
                    @if($query->status == 'TERMINATED')
                    <a href="{{ URL::to('https://thesisdata.s3.amazonaws.com/output/'.$user->name.'/'.$query->path.'/part-r-00000') }}">Download</a>
                    @endif
                  </td>
                </tr>
              @endforeach
            </tbody>
            </table>
          </div>
          <a href="queries/create"><button type="button" name="newQuery" class="btn btn-primary form-control">Create a new query</button></a>
        </div>
  </div>
</div>
@stop
