# -*- coding: utf-8 -*-

from diagrams import Cluster, Diagram, Edge

from diagrams.k8s.infra import ETCD
from diagrams.k8s.controlplane import APIServer
from diagrams.k8s.compute import Pod, StatefulSet
from diagrams.k8s.network import Service
from diagrams.k8s.compute import Deployment
from diagrams.k8s.storage import Vol, PV, PVC, StorageClass
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
    with Diagram(name="Vault Agent Architecture", show=False, direction="LR"):

        with Cluster("Control Plane"):
            apiserver = APIServer()

        with Cluster("Vault"):
            svc = Service(":8200")
            certs_secret = Secret("Certs Secret")
            vault_configmap = ConfigMap("Vault Config")
            vault = Vault("Vault")
            file_backend = PV("Encrypted Store")

            vault >> file_backend
            vault >> Edge() << svc
            vault << certs_secret
            vault << vault_configmap

        with Cluster("Secure Pod"):
            vault_agent = Custom("Vault Agent", crio_icon)
            app_container = Custom("App", crio_icon)
            inMemory = Vol("In Memory")

            vault_agent >> inMemory
            app_container << inMemory

            vault_agent << svc << vault_agent >> Edge() << app_container
        
        apiserver >> Edge() << vault

def webhooked_vault_agent_architecture():
    with Diagram(name="Mutating Webhook", show=False):
        with Cluster("Control Plane"):
            apiserver = APIServer()

        with Cluster("Mutating Webhook"):
            webhook = SQS("Mutating Webhook")

        with Cluster("Vault"):
            vault = Vault("Vault")

        with Cluster("Secure Pod"):
            with Cluster("Injected"):
                vault_init_agent = Custom("Init Vault Agent", crio_icon)
                vault_agent = Custom("Vault Agent", crio_icon)
                injected = [vault_init_agent, vault_agent]

            app_container = Custom("App", crio_icon)
            inMemory = Vol("In Memory")

            vault_init_agent >> inMemory
            app_container << inMemory

            vault >> Edge() << vault_agent >> Edge() << app_container

        apiserver >> Edge() << webhook
        apiserver >> Edge() << vault
        webhook >> vault_agent

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
