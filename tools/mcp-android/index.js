#!/usr/bin/env node

import { spawn } from 'node:child_process'
import { Server } from '@modelcontextprotocol/sdk/server/index.js'
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js'
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js'
import path from 'node:path'
import os from 'node:os'

// Project/app defaults
const PROJECT_ROOT = process.env.PROJECT_ROOT || process.cwd()
const DEFAULT_APP_ID = process.env.APP_ID || 'com.example.privytaskai'
const DEFAULT_ACTIVITY = process.env.APP_ACTIVITY || '.presentation.MainActivity'

// Ensure ANDROID_HOME/platform-tools is on PATH for adb
const home = os.homedir()
const defaultAndroidHome = path.join(home, 'Library', 'Android', 'sdk')
if (!process.env.ANDROID_HOME) {
  process.env.ANDROID_HOME = defaultAndroidHome
}
const platformTools = path.join(process.env.ANDROID_HOME, 'platform-tools')
if (process.env.PATH && !process.env.PATH.includes(platformTools)) {
  process.env.PATH = `${platformTools}:${process.env.PATH}`
}

function execCmd(cmd, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { cwd: PROJECT_ROOT, env: process.env, shell: false, ...options })
    let stdout = ''
    let stderr = ''
    child.stdout.on('data', (d) => (stdout += d.toString()))
    child.stderr.on('data', (d) => (stderr += d.toString()))
    child.on('error', (err) => reject(err))
    child.on('close', (code) => {
      if (code === 0) resolve({ code, stdout: stdout.trim(), stderr: stderr.trim() })
      else reject(new Error(`Command failed (${code}): ${cmd} ${args.join(' ')}\n${stderr || stdout}`))
    })
  })
}

function sanitizePackageName(name) {
  if (!name) return DEFAULT_APP_ID
  if (!/^[A-Za-z0-9_.]+$/.test(name)) throw new Error('Invalid package name')
  return name
}

function sanitizeActivity(activity) {
  if (!activity) return DEFAULT_ACTIVITY
  if (!/^([A-Za-z0-9_.]+|\.[A-Za-z0-9_.]+)$/.test(activity)) throw new Error('Invalid activity name')
  return activity
}

function withSerial(serial, baseArgs) {
  if (serial) {
    if (!/^[A-Za-z0-9._:-]+$/.test(serial)) throw new Error('Invalid device serial')
    return ['-s', serial, ...baseArgs]
  }
  return baseArgs
}

const tools = [
  {
    name: 'adb_devices',
    description: 'List connected devices/emulators (adb devices)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      const res = await execCmd('adb', ['devices'])
      return res.stdout
    },
  },
  {
    name: 'adb_launch_app',
    description: 'Launch the app activity on emulator/device (adb shell am start -n <pkg>/<activity>)',
    inputSchema: {
      type: 'object',
      properties: {
        package: { type: 'string', description: 'Application id (default: project appId)' },
        activity: { type: 'string', description: 'Activity name, may be fully-qualified or start with dot' },
        userId: { type: 'number', description: 'Android user id (optional)' },
        serial: { type: 'string', description: 'adb device serial to target (optional)' },
      },
      additionalProperties: false,
    },
    handler: async (args) => {
      const pkg = sanitizePackageName(args.package)
      const act = sanitizeActivity(args.activity)
      const comp = `${pkg}/${act.startsWith('.') ? pkg + act : act}`
      let adbArgs = ['shell', 'am', 'start', '-n', comp]
      if (args.userId != null) adbArgs.unshift('--user', String(args.userId))
      adbArgs = withSerial(args.serial, adbArgs)
      const res = await execCmd('adb', adbArgs)
      return res.stdout || 'Started'
    },
  },
  {
    name: 'adb_force_stop_app',
    description: 'Force-stop an app (adb shell am force-stop <pkg>)',
    inputSchema: {
      type: 'object',
      properties: { package: { type: 'string' }, serial: { type: 'string' } },
      required: [],
      additionalProperties: false,
    },
    handler: async (args) => {
      const pkg = sanitizePackageName(args.package)
      const adbArgs = withSerial(args.serial, ['shell', 'am', 'force-stop', pkg])
      const res = await execCmd('adb', adbArgs)
      return res.stdout || 'Force-stopped'
    },
  },
  {
    name: 'adb_clear_app_data',
    description: 'Clear app user data (adb shell pm clear <pkg>)',
    inputSchema: {
      type: 'object',
      properties: { package: { type: 'string' }, serial: { type: 'string' } },
      required: [],
      additionalProperties: false,
    },
    handler: async (args) => {
      const pkg = sanitizePackageName(args.package)
      const adbArgs = withSerial(args.serial, ['shell', 'pm', 'clear', pkg])
      const res = await execCmd('adb', adbArgs)
      return res.stdout
    },
  },
  {
    name: 'adb_logcat_tail',
    description: 'Show the last N lines of logcat, optionally filtered by tag and level.',
    inputSchema: {
      type: 'object',
      properties: {
        tag: { type: 'string', description: 'Filter by tag (optional)' },
        level: { type: 'string', enum: ['V', 'D', 'I', 'W', 'E', 'F', 'S'], description: 'Log level (optional)' },
        lines: { type: 'number', description: 'Number of lines to tail (default 200)' },
        serial: { type: 'string', description: 'adb device serial to target (optional)' },
      },
      additionalProperties: false,
    },
    handler: async (args) => {
      const { tag, level, lines, serial } = args || {}
      const count = lines && Number.isFinite(lines) ? String(lines) : '200'
      const filterSpec = tag ? `${tag}:${level || 'V'}` : '*:I'
      const adbArgs = withSerial(serial, ['logcat', '-d', '-T', count, filterSpec])
      const res = await execCmd('adb', adbArgs)
      return res.stdout
    },
  },
  {
    name: 'gradle_installDebug',
    description: 'Build and install the debug APK on the connected device/emulator (./gradlew installDebug)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
      const res = await execCmd(gradlew, ['installDebug'])
      return res.stdout
    },
  },
  {
    name: 'gradle_connectedAndroidTest',
    description: 'Run connected Android tests on the emulator/device (./gradlew connectedDebugAndroidTest)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
      const res = await execCmd(gradlew, ['connectedDebugAndroidTest'])
      return res.stdout
    },
  },
  {
    name: 'app_open_deeplink',
    description: 'Open a deeplink URI within the app (adb shell am start -a VIEW -d <uri> <pkg>)',
    inputSchema: {
      type: 'object',
      properties: {
        uri: { type: 'string' },
        package: { type: 'string' },
        serial: { type: 'string' },
      },
      required: ['uri'],
      additionalProperties: false,
    },
    handler: async (args) => {
      const pkg = sanitizePackageName(args.package)
      const adbArgs = withSerial(args.serial, ['shell', 'am', 'start', '-a', 'android.intent.action.VIEW', '-d', String(args.uri), pkg])
      const res = await execCmd('adb', adbArgs)
      return res.stdout
    },
  },
]

const server = new Server(
  { name: 'mcp-android-tools', version: '0.1.0' },
  { capabilities: { tools: {} } }
)

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: tools.map((t) => ({ name: t.name, description: t.description, inputSchema: t.inputSchema })),
}))

server.setRequestHandler(CallToolRequestSchema, async (req) => {
  const name = req.params.name
  const args = req.params.arguments || {}
  const tool = tools.find((t) => t.name === name)
  if (!tool) {
    return { content: [{ type: 'text', text: `Unknown tool: ${name}` }], isError: true }
  }
  try {
    const out = await tool.handler(args)
    return { content: [{ type: 'text', text: String(out ?? '') }] }
  } catch (err) {
    return { content: [{ type: 'text', text: `Error: ${err?.message || String(err)}` }], isError: true }
  }
})

await server.connect(new StdioServerTransport())
