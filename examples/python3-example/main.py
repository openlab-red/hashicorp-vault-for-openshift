# -*- coding: utf-8 -*-
""" Example python demo app """

import logging
import os
import json
from flask import Flask

LOG = logging.getLogger('werkzeug')
LOG.setLevel(logging.ERROR)

APP_ROOT = os.path.dirname(os.path.abspath(__file__))

FLASK_APP = Flask(__name__)

@FLASK_APP.route('/secret', methods=['GET'])
def get_secret(secret_file_path='resources/application.txt'):
    """ prints secret data as read from specified file """

    if not os.path.exists(os.path.join(APP_ROOT, secret_file_path)):
        return json.dumps({'File not found': False}), 404

    with open(os.path.join(APP_ROOT, secret_file_path)) as f:
        return f.read()

@FLASK_APP.route('/healthz', methods=['GET'])
def check_healthz():
    """ basic kubernetes healthcheck """

    return json.dumps({'success':True}), 200

@FLASK_APP.route('/env', methods=['GET'])
def get_environment_variables():
    """ dumps a json dict built from python3 environment variables """

    config_location = os.environ.get('PYTHON3_CONFIG_LOCATION')
    additional_config_location = os.environ.get('PYTHON3_CONFIG_ADDITIONAL_LOCATION')

    return(json.dumps({
        'PYTHON3_CONFIG_LOCATION': config_location,
        'PYTHON3_CONFIG_ADDITIONAL_LOCATION': additional_config_location
        }))

@FLASK_APP.route('/', methods=['GET'])
def get_api():
    """ returns available api resources """

    return json.dumps({
        '/secret': 'reveals the stored secret',
        '/healthz': 'healthcheck endpoint',
        '/env': 'debug environment variables'
    })


if __name__ == "__main__":
    FLASK_APP.run(host='0.0.0.0', port='8080')
