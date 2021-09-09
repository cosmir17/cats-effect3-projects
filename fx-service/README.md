![Build Status](https://codebuild.eu-west-1.amazonaws.com/badges?uuid=eyJlbmNyeXB0ZWREYXRhIjoiTkVYcmJnZWJEN3JzWU1ZMzBoUyt4UVg1bmo2ejByNndIQzUvVkZSaTA5NlRuMTkwb0tPUzNKV0l5bnFxQWNHOTJuY0Z0Qkx3U0VvQURJbHQrUHJtcmRJPSIsIml2UGFyYW1ldGVyU3BlYyI6IjBMTnh3VjdOdXlmUFUzQUciLCJtYXRlcmlhbFNldFNlcmlhbCI6MX0%3D&branch=master)

Sean's Foreign Exchange Service
=============
This project uses Cats Effect 3, Cats, Http4s and Decline.

## Components Overview
Here's an overview of the three different components that make this application.

- Main : Main class file.
- FxConverter : Where the core logic is
- converter : Where all the case classes are with encoders/decoders
- FxClient : Where a downstream request is made

## How to run
execute the Main class.

## How to run Tests
sbt clean it:test test

"VH_APP_ENV" is set as "test" in the build file.

P.S. This project is based on https://github.com/gvolpe/pfps-shopping-cart
This is a completely different application.