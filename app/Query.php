<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Query extends Model
{
    protected $fillable = [
      'user_id',
      'query_type',
      'status',
      'path',
      'request_time',
      'query_time',
    ];

    protected $guarded = [
      'id',
    ];

    public function user() {
      return $this->belongsTo('App\User');
    }
}
