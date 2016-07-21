<?php

namespace App\Http\Controllers;

use Auth;

use App\Http\Requests\QueryRequest;

use Illuminate\Http\Request;

use App\Query;

use Carbon\Carbon;

class QueryController extends Controller
{


  public function __construct() {
      $this->middleware('auth');
  }

    public function index() {

      $user = Auth::user();
      $queries = Query::latest()->get()->where('user_id',$user->id);

      return view('queries.index', compact('queries'));
    }

    public function show($id) {
      $query = Query::findorfail($id);
      return view('queries.show', compact('query'));
    }

    public function create() {

      return view('queries.create');
    }

    public function edit($id) {
      $query = Query::findorfail($id);

      return view('queries.edit',compact('query'));
    }

    public function store(QueryRequest $request) {
      //validation happens here

      $user = Auth::user();

      $path = $request->input('filename');
      $query_time = Carbon::parse($request->input('query_date').' '.$request->input('query_time'));

      $result = buildQuery1($query_time->toDateTimeString(), $path);

      Query::create(array(
                            'job_flow_id' => $result,
                            'user_id' => $user->id,
                            'query_type' => $request->input('query_type'),
                            'path' => $path,
                            'query_time' => $query_time,
                          ));

     return redirect('queries')->with('status','CREATED');
    }

    public function update($id, QueryRequest $request)
    {

      $user = Auth::user();
      $query = Query::findorfail($id);
      $query->update(array(
                            'user_id' => $user->id,
                            'query_type' => $request->input('query_type'),
                            'path' => $request->input('filename'),
                            'query_time' => Carbon::parse($request->input('query_date').' '.$request->input('query_time')),
                          ));

      return redirect('queries')->with('status','UPDATED');
    }

}
