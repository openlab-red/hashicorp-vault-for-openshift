import logging
import os
import json
from flask import Flask

log = logging.getLogger('werkzeug')
log.setLevel(logging.ERROR)

app_root = os.path.dirname(os.path.abspath(__file__))

flask_app = Flask(__name__)

@flask_app.route('/secret', methods=['GET'])
def get_secret():
    with open(os.path.join(app_root, "resources/application.txt")) as f:
        return(f.read())

@flask_app.route('/healthz', methods=['GET'])
def check_healthz():
    return json.dumps({'success':True}), 200, {'ContentType':'application/json'}

if __name__ == "__main__":
    flask_app.run(host='0.0.0.0', port='8080')