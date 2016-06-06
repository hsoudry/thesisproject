<?php

namespace App\Http\Controllers;

use Auth;

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

    public function store(Request $request) {
      $user = Auth::user();

      $this->validate($request, array(
                                      'query_type' => 'required',
                                      //'query_date' => 'required|date|after:01/01/2013|before:31/12/2013',
                                      //'query_time' => 'required|date',
                                      'filename' => 'required|max:255|unique:queries,path,NULL,id,user_id,'.$user->id,
                                      ));

      Query::create(array(
                            'user_id' => $user->id,
                            'query_type' => $request->input('query_type'),
                            'request_time' => Carbon::now(),
                            'path' => $request->input('filename'),
                            'query_time' => Carbon::parse($request->input('query_date').' '.$request->input('query_time')),
                          ));
     return redirect('queries');
    }
}
