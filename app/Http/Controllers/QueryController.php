<?php

namespace App\Http\Controllers;

use Auth;

use Illuminate\Http\Request;

use App\Http\Requests;

use App\Query;

class QueryController extends Controller
{

  public function __construct() {
      $this->middleware('auth');
  }

    public function index() {

      $user = Auth::user();
      $queries = Query::all()->where('user_id',$user->id);

      return view('queries.index', compact('queries'));
    }
}
