<?php

namespace App\Http\Requests;

use App\Http\Requests\Request;

use Auth;

class QueryRequest extends Request
{
    /**
     * Determine if the user is authorized to make this request.
     *
     * @return bool
     */
    public function authorize()
    {
        return true;
    }

    /**
     * Get the validation rules that apply to the request.
     *
     * @return array
     */
    public function rules()
    {
      $user = Auth::user();

    switch($this->method())
    {
        case 'GET':
        case 'DELETE':
        {
            return [];
        }
        case 'POST':
        {
            return [
              'query_type' => 'required',
              'query_date' => 'required|date_format:Y-m-d|after:2012-12-31|before:2014-01-01',
              'query_time' => 'required|date_format:H:i',
              'filename' => 'required|max:255|unique:queries,path,NULL,id,user_id,'.$user->id,
            ];
        }
        case 'PUT':
        case 'PATCH':
        {
            return [
              'query_type' => 'required',
              'query_date' => 'required|date_format:Y-m-d|after:2012-12-31|before:2014-01-01',
              'query_time' => 'required|date_format:H:i',
              'filename' => 'required|max:255|unique:queries,path,'.$this->segment(2).',id,user_id,'.$user->id,
            ];
        }
        default:break;
    }
    }
}
