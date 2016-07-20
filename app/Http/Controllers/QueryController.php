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

      $result = buildQuery1($query_time, $path);

      Query::create(array(
                            'user_id' => $user->id,
                            'query_type' => $request->input('query_type'),
                            'path' => $path,
                            'query_time' => $query_time,
                          ));

     return redirect('queries')->with('status','CREATED')->with('result',$result);
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

    private function buildQuery1(String $query_time, String $path) {
      $client = AWS::createClient('EMR');

      $result = $client->runJobFlow([
        'AmiVersion' => '4.7.1',
        'Applications' => [
          [
            'Name' => 'Hadoop',
            'Version' => '2.7.2',
          ],
        ],
        'Instances' => [
          'Ec2KeyName' => 'EMR key',
          'InstanceGroups' => [
            [
              'Configurations' => [
                [
                  'Classification' => 'hadoop-env',
                  'Configurations' => [
                    'Classification' => 'export',
                    'Configurations' => [],
                    'Properties' => ['JAVA_HOME' => '/usr/lib/jvm/java-1.8.0'],
                  ],
                  'Properties' => [],
                ],
              ],
              'InstanceCount' => 1,
              'InstanceRole' => 'MASTER',
              'InstanceType' => 'm3.xlarge',
            ],
            [
              'Configurations' => [
                [
                  'Classification' => 'hadoop-env',
                  'Configurations' => [
                    'Classification' => 'export',
                    'Configurations' => [],
                    'Properties' => ['JAVA_HOME' => '/usr/lib/jvm/java-1.8.0'],
                  ],
                  'Properties' => [],
                ],
              ],
              'InstanceCount' => 2,
              'InstanceRole' => 'CORE',
              'InstanceType' => 'm3.xlarge',
            ],
          ],
        ],
        'Name' => 'Query 1 cluster',
        'Steps' => [
          [
            'ActionOnFailure' => 'CONTINUE',
            'HadoopJarStep' => [
              'Args' => ['s3a://thesisdata/input', 's3a://thesisdata/output/'+$path, $query_time],
              'Jar' => 's3a://thesisdata/jar/TopTenRoutes.jar',
              'MainClass' => 'TopTenRoutes',
            ],
            'LogUri' => 's3a://thesisdata/logs/',
            'Name' => 'Top ten computation',
          ],
        ],
      ]);
      return $result;
    }

}
