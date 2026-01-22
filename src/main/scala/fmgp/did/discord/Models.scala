package fmgp.did.discord

// User model - kept here since it's part of the login flow
case class User(id: String, secret: Int, loginStatus: Boolean)
