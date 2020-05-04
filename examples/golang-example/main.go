package main

import (
	"encoding/json"
	"io/ioutil"
	"net/http"
	"os"

	log "github.com/sirupsen/logrus"
	"sigs.k8s.io/yaml"
)

type Config struct {
	Secret string `yaml:"secret"`
}

type Secrets struct {
	path string
}

func init() {
	// log as JSON
	log.SetFormatter(&log.JSONFormatter{})

	// Output everything including stderr to stdout
	log.SetOutput(os.Stdout)

	// set level
	log.SetLevel(log.InfoLevel)
}

func (s *Secrets) secretsHandler(w http.ResponseWriter, req *http.Request) {

	yamlFile, err := ioutil.ReadFile((*s).path)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte(err.Error()))
		log.Error(err.Error())
	}
	var config Config
	err = yaml.Unmarshal(yamlFile, &config)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte(err.Error()))
		log.Error(err.Error())
	}

	w.Write([]byte("The secret is: " + config.Secret))
	log.Info(req.Method + req.RequestURI + " " + req.Proto)
}

func helloHandler(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "text/html")
	w.Write([]byte("hello"))
	log.Info(req.Method + req.RequestURI + " " + req.Proto)
}

func headersHandler(w http.ResponseWriter, req *http.Request) {
	jsonHeaders, err := json.Marshal((*req).Header)
	if err != nil {
		log.Error(err.Error())
	}
	w.Header().Set("Content-Type", "application/json")
	w.Write(jsonHeaders)
	log.Info(req.Method + req.RequestURI + " " + req.Proto)
}

func main() {

	port := ":8080"

	s := Secrets{
		//path: "resources/config.yaml",
		path: "/resources/config.yaml",
	}
	http.HandleFunc("/secrets", s.secretsHandler)
	http.HandleFunc("/hello", helloHandler)
	http.HandleFunc("/headers", headersHandler)

	log.Info("Listening on port ", port)
	log.Fatal(http.ListenAndServe(port, nil))
}
