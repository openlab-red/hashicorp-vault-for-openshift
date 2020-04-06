# -*- coding: utf-8 -*-

from diagrams import Cluster, Diagram, Edge

from diagrams.k8s.infra import ETCD
from diagrams.k8s.controlplane import APIServer
from diagrams.k8s.compute import Pod, StatefulSet
from diagrams.k8s.network import Service
from diagrams.k8s.compute import Deployment
from diagrams.k8s.storage import PV, PVC, StorageClass
from diagrams.k8s.podconfig import Secret, ConfigMap

from diagrams.onprem.security import Vault

from diagrams.onprem.database import PostgreSQL

# Fake Admission Webhook
from diagrams.aws.integration import SQS

from diagrams.custom import Custom

import os
import argparse

APP_ROOT = os.path.dirname(os.path.abspath(__file__))
crio_icon = os.path.join(APP_ROOT, "resources/cri-o.png")

def basic_vault_agent_architecture():
    with Diagram(name="Vault Agent Architecture", show=False):
        apiserver = APIServer()

        with Cluster("Vault"):
            svc = Service(":8200")
            certs_secret = Secret("Certs Secret")
            vault_configmap = ConfigMap("Vault-ConfigMap")
            vault_dc = Deployment("DC")
            vault = Vault("Vault")
            file_backend = PV("File Backend")
            encrypted_store = PostgreSQL("Encrypted Store")

            vault_apps = []
            apiserver << Edge(label="verify SA token", color="orange") << vault
            vault_apps.append(vault >> svc << vault >> file_backend >> encrypted_store)
            certs_secret << Edge() << vault
            vault_configmap << Edge() << vault

        with Cluster("Secure Pod"):
            vault_agent = Custom("Vault-Agent", crio_icon)
            app_container = Custom("App-Container", crio_icon)
            vault_agent >> Edge() << app_container
            svc << Edge() << vault_agent
            svc << Edge(label="use token to get secrets", color="orange") << vault_agent


def webhooked_vault_agent_architecture():
    with Diagram(name="Mutating Webhook", show=False):
        with Cluster("Control Plane"):
            apiserver = APIServer()
            etcd = ETCD()
            apiserver >> Edge() << etcd

        with Cluster("Vault"):
            svc = Service(":8200")
            certs_secret = Secret("Certs")
            vault_configmap = ConfigMap("Vault ConfigMap")
            vault_dc = Deployment("Deployment")
            vault = Vault("Vault")
            file_backend = PV("File Backend")
            encrypted_store = PostgreSQL("Encrypted Store")

            vault_apps = []
            apiserver << Edge(label="verify SA token", color="orange") << vault
            vault_apps.append(vault >> svc << vault >> file_backend >> encrypted_store)
            certs_secret << Edge() << vault
            vault_configmap << Edge() << vault

        with Cluster("Secure Pod"):
            vault_agent = Custom("Vault-Agent", crio_icon)
            vault_agent_init = Custom("Init-Vault-Agent", crio_icon)
            app_container = Custom("App-Container", crio_icon)
            vault_agent >> Edge() << app_container
            svc << Edge() << vault_agent
            svc << Edge(label="Use token to get secrets", color="orange") << vault_agent

        webhook = SQS("Mutating Webhook")
        apiserver >> Edge(label="Notified at pod creation") >> webhook >> Edge(label="Injects") >> vault_agent

def main():
    parser = argparse.ArgumentParser(
        description="Generates different Vault diagrams")
    parser.add_argument("--type", "-t",
        default="all",
        help="basic | webhook | all (default)")

    args = parser.parse_args()

    if args.type == "all":
        basic_vault_agent_architecture()
        webhooked_vault_agent_architecture()
    elif args.type == "webhook":
        webhooked_vault_agent_architecture()
    else:
        basic_vault_agent_architecture()

if __name__ == "__main__":
    main()
