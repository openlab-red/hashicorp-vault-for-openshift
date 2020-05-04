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
	/*
		filename, errPath := filepath.Abs("/secrets/example.yaml")
		if errPath != nil {
			log.Info("Cannot find yaml conifg file /secrets/example.yaml. Fallback")
		}
	*/

	yamlFile, err := ioutil.ReadFile((*s).path)
	var config Config
	err = yaml.Unmarshal(yamlFile, &config)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte(err.Error()))
	}

	w.Write([]byte("The secret is: " + config.Secret))
	log.Infof("%s %s %s", req.Method, req.RequestURI, req.Proto)
}

func helloHandler(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "text/html")
	w.Write([]byte("hello"))
	log.Infof("%s %s %s", req.Method, req.RequestURI, req.Proto)
}

func headersHandler(w http.ResponseWriter, req *http.Request) {
	jsonHeaders, err := json.Marshal((*req).Header)
	if err != nil {
		log.Error(err.Error())
	}
	w.Header().Set("Content-Type", "application/json")
	w.Write(jsonHeaders)
	log.Infof("%s %s %s", req.Method, req.RequestURI, req.Proto)
	/*
		for name, value := range (*req).Header {
			response := name + ":" + strings.Join(value, ",") + "\n"
			w.Write([]byte(response))
		}
	*/
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
