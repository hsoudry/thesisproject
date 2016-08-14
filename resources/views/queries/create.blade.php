@extends('layouts.app')

@section('content')
<div class="container">
  <div class="row">
    <div class="col-xs-12">
      <h1>Create a new Query</h1>
      <hr/>
      {!! Form::open(array('action' => 'QueryController@store')) !!}

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
        {!! Form::date('query_date', \Carbon\Carbon::createFromDate(2013,1,1), array('class'=>'form-control')) !!}<br>
        {!! Form::label('query_date','Query search time:') !!}
        {!! Form::time('query_time', '12:00' , array('class'=>'form-control')) !!}
      </div>

    <div class="form-group">
      {!! Form::label('filename','Output file name:') !!}
      {!! Form::text('filename','output_'.Auth::user()->name.'_'.\Carbon\Carbon::now()->timestamp, array('class'=>'form-control')) !!}
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
</div>
@stop
