<?php

use Aws\Laravel\AwsServiceProvider;

return [

    /*
    |--------------------------------------------------------------------------
    | AWS SDK Configuration
    |--------------------------------------------------------------------------
    |
    | The configuration options set in this file will be passed directly to the
    | `Aws\Sdk` object, from which all client objects are created. The minimum
    | required options are declared here, but the full set of possible options
    | are documented at:
    | http://docs.aws.amazon.com/aws-sdk-php/v3/guide/guide/configuration.html
    |
    */
    'credentials' => [
        'key'    => 'AKIAI3S3VFC2QIZKF5ZA',
        'secret' => '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',
    ],
    'region' => env('AWS_REGION', 'eu-west-1'),
    'version' => 'latest',

    // email services
    'Ses' => [
        'region' => 'eu-west-1',
    ],
    'ua_append' => [
        'L5MOD/' . AwsServiceProvider::VERSION,
    ],
];
