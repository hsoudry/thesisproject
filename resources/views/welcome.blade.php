@extends('layouts.app')

@section('content')
<div class="container">
    <div class="row">
        <div class="col-xs-12">
            <div class="panel panel-default">
                <div class="panel-heading">Welcome</div>

                <div class="panel-body">
                    This application was developed as part of my thesis project. The goal of this web application is to make the creation of big data queries on the Amazon Cloud easier.
                    The application was developed using the Laravel Framework and is deployed on the Amazon Elastic Beanstalk. The big data requests are handled by Amazon Elastic MapReduce.
                </div>
            </div>
        </div>
    </div>
    @if(!Auth::check())
      <div class="row">
        <div class="col-md-10 col-md-offset-1">
          <div class="col-xs-6">
            <a href="/register"><button type="button" class="btn btn-success btn-lg btn-block" name="button">Register</button></a>
          </div>
          <div class="col-xs-6">
            <a href="/login"><button type="button" class="btn btn-info btn-lg btn-block" name="button">Login</button></a>
          </div>
        </div>
      </div>
    @endif
</div>
@endsection
