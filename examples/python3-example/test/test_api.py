from flask import Flask, request

FLASK_APP = Flask(__name__)

@FLASK_APP.route('/secret')
def test_get_secret():
    with FLASK_APP.test_client() as client:
        response = client.get('/secret')
        json_data = response.get_json()
        assert verify_password(json_data)

def verify_password(json_data):
    if 'pwd' in json_data is not None:
        return json_data