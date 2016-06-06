@extends('layouts.app')

@section('content')
<div class="row">
  <div class="col-xs-12 col-sm-offset-1 col-sm-10">
    <h1>Edit your query</h1>
    <hr/>
    {!! Form::model($query, array('method' => 'PATCH', 'action' => ['QueryController@update',$query->id])) !!}

    <div class="form-group">
      <div class="row">
        <div class="col-sm-6">
          {!! Form::radio('query_type','1',true, array('class' => 'radio-inline','style'=>'vertical-align: middle; margin: 0px;')) !!}
          {!! Form::label('1','Query 1') !!}
        </div>
        <div class="col-sm-6">
          {!! Form::radio('query_type','2', false, array('class' => 'radio-inline','style'=>'vertical-align: middle; margin: 0px;')) !!}
          {!! Form::label('2','Query 2') !!}
        </div>
      </div>


  </div>

    <div class="form-group">
      {!! Form::label('query_date','Query search date:') !!}
      {!! Form::date('query_date', \Carbon\Carbon::parse($query->query_time) , array('class'=>'form-control')) !!}<br>
      {!! Form::label('query_date','Query search time:') !!}
      {!! Carbon\Carbon::setToStringFormat('H:i'); !!}
      {!! Form::time('query_time', \Carbon\Carbon::parse($query->query_time) , array('class'=>'form-control')) !!}
    </div>

  <div class="form-group">
    {!! Form::label('filename','Output file name:') !!}
    {!! Form::text('filename',$query->path, array('class'=>'form-control')) !!}
  </div>


    {!! Form::submit('Submit', array('class' => 'btn btn-primary form-control')) !!}
    {!! Form::close() !!}
    <br>
    @if ($errors->any())
    <div class="form-group">
      <div class=" alert alert-danger">
        <ul>
          @foreach($errors->all() as $error)
            <li>{{$error}}</li>
          @endforeach
        </ul>
      </div>
    </div>
    @endif
  </div>
</div>
@stop
