<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Query extends Model
{
    protected $fillable = [
      'user_id',
      'query_type',
      'status',
      'request_time'
    ];

    public function user() {
      return $this->belongsTo('App\User');
    }
}
