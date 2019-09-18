# Indoqa - Nexus Downloader


A system for downloading and installing artefacts from a Nexus or similar maven-based repository.

The actual URL of the repository, credentials, and artefacts are managed on the nexus downloader server. The nexus downloader client retrieves these settings only when needed without storing them locally, helping to keep everything consistent and secure across deployment servers.