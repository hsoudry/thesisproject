<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

use App\Http\Requests;

use App\Query;

class QueryController extends Controller
{
    public function index() {
      $queries = Query::all();
      
      return view('queries.index', compact('queries'));
    }
}
