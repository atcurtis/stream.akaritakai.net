<template>
  <table class="table max100w">
    <tbody>
    <tr><td width="50%">
      <div class="form-group">
      <input v-model="selectionFilter" type="text" class="form-control" id="prefix"
           placeholder="Filter/Search - max 10 results">
      <br>
      <b-table class="scroll-x max50wt" small head-variant="light"
           id="stream-entries" ref="streamEntries"
           :busy.sync="form.selection.inProgress"
           primary-key="name"
           :fields="form.selection.fields"
           select-mode="single"
           :items="provideStreamEntries"
           responsive="sm" selectable
           @row-selected="onRowSelected">
        <template #table-busy>
          <div class="text-center text-danger my-2">
            <strong>Please wait...</strong>
          </div>
        </template>
        <template #cell(name)="data">
          {{ data.value }}
        </template>
        <template #cell(desc)="data">
          {{ data.item.metadata.name }}
        </template>
        <template #cell(duration)="data">
          {{ liveOrDuration(data.item.metadata) }}
        </template>
      </b-table>
      </div>
    </td>
    <td width="50%">
      <b-form v-if="!needApiKey && streamStopped" @submit.stop.prevent="alwaysTrue">
        <div class="form-group">
          <label for="startName" class="form-label">Name</label>
          <input v-model="form.start.name" type="text" class="form-control max50w" id="startName" placeholder="The name of the media to start" required>
        </div>
        <div class="form-group">
          <label for="startSeekTime" class="form-label">Seek Time</label>
          <input v-model="form.start.seekTime" type="text" class="form-control max50w" id="startSeekTime" placeholder="(Optional) The time to seek to in the media as seconds, [mm:ss], or [hh:mm:ss]">
        </div>
        <div class="form-group">
          <label for="startStartAt" class="form-label">Start At</label>
          <input v-model="form.start.startAt" type="text" class="form-control max50w" id="startStartAt" placeholder="(Optional) The time to start at as [hh:mm a] (e.g. '03:45 pm')">
        </div>
        <div class="form-group">
          <label for="startDelay" class="form-label">Delay</label>
          <input v-model="form.start.delay" type="text" class="form-control max50w" id="startDelay" placeholder="(Optional) The delay in seconds to start">
        </div>
        <div class="form-group">
          <button type="submit" class="btn btn-primary" @click="startStream" v-if="!form.start.inProgress">Start Stream</button>
          <button type="submit" class="btn btn-primary" v-if="form.start.inProgress" disabled>
            <b-spinner small/> Starting...
          </button>
        </div>
      </b-form>
      <b-form v-if="!needApiKey && streamStartingSoon" @submit.stop.prevent="alwaysTrue">
        <div class="form-group">
          <button type="submit" class="btn btn-primary" @click="stopStream" v-if="!form.stop.inProgress">Stop Stream</button>
          <button type="submit" class="btn btn-primary" v-if="form.stop.inProgress" disabled>
            <b-spinner small/> Stopping...
          </button>
        </div>
      </b-form>
      <b-form v-if="!needApiKey && streamRunning" @submit.stop.prevent="alwaysTrue">
        <div class="form-group">
          <button type="submit" class="btn btn-primary" @click="pauseStream" v-if="!form.pause.inProgress">Pause Stream</button>
          <button type="submit" class="btn btn-primary" v-if="form.pause.inProgress" disabled>
            <b-spinner small/> Pausing...
          </button>
          <button type="submit" class="btn btn-primary" @click="stopStream" v-if="!form.stop.inProgress">Stop Stream</button>
          <button type="submit" class="btn btn-primary" v-if="form.stop.inProgress" disabled>
            <b-spinner small/> Stopping...
          </button>
        </div>
      </b-form>
      <b-form v-if="!needApiKey && streamPaused" @submit.stop.prevent="alwaysTrue">
        <div class="form-group">
          <label for="resumeSeekTime" class="form-label">Seek Time</label>
          <input v-model="form.resume.seekTime" type="text" class="form-control max50w" id="resumeSeekTime" placeholder="(Optional) The time to seek to in the media as seconds, [mm:ss], or [hh:mm:ss]">
        </div>
        <div class="form-group">
          <label for="resumeStartAt" class="form-label">Start At</label>
          <input v-model="form.resume.startAt" type="text" class="form-control max50w" id="resumeStartAt" placeholder="(Optional) The time to start at as [hh:mm a] (e.g. '03:45 pm')">
        </div>
        <div class="form-group">
          <label for="resumeDelay" class="form-label">Delay</label>
          <input v-model="form.resume.delay" type="text" class="form-control max50w" id="resumeDelay" placeholder="(Optional) The delay in seconds to start">
        </div>
        <div class="form-group">
          <button type="submit" class="btn btn-primary" @click="resumeStream" v-if="!form.resume.inProgress">Resume Stream</button>
          <button type="submit" class="btn btn-primary" v-if="form.resume.inProgress" disabled>
            <b-spinner small/> Resuming...
          </button>
          <button type="submit" class="btn btn-primary" @click="stopStream" v-if="!form.stop.inProgress">Stop Stream</button>
          <button type="submit" class="btn btn-primary" v-if="form.stop.inProgress" disabled>
            <b-spinner small/> Stopping...
          </button>
        </div>
      </b-form>
      <b-form v-if="!needApiKey && enabled" @submit.stop.prevent="alwaysTrue">
        <div class="form-group">
          <button type="submit" class="btn btn-primary" @click="disableChat" v-if="!form.disableChat.inProgress">Disable Chat</button>
          <button type="submit" class="btn btn-primary" v-if="form.disableChat.inProgress" disabled>
            <b-spinner small/> Disabling...
          </button>
        </div>
      </b-form>
      <b-form v-if="!needApiKey && !enabled" @submit.stop.prevent="alwaysTrue">
        <div class="form-group">
          <button type="submit" class="btn btn-primary" @click="enableChat" v-if="!form.enableChat.inProgress">Enable Chat</button>
          <button type="submit" class="btn btn-primary" v-if="form.enableChat.inProgress" disabled>
            <b-spinner small/> Enabling...
          </button>
        </div>
      </b-form>
      <b-form v-if="!needApiKey" @submit.stop.prevent="alwaysTrue">
        <div class="form-group">
          <button type="submit" class="btn btn-primary" @click="resetApiKey">Reset API Key</button>
        </div>
      </b-form>
    </td></tr>
    </tbody>
  </table>
</template>

<script>
  import {mapGetters, mapState} from 'vuex';
  import videojs from 'video.js/core';
  import { DateTime } from 'luxon';
  import axios from 'axios';

  export default {
    name: 'stream-status',
    data () {
      return {
        form: {
          start: {
            inProgress: false,
            name: '',
            delay: '',
            seekTime: '',
            startAt: ''
          },
          pause: {
            inProgress: false
          },
          resume: {
            inProgress: false,
            delay: '',
            seekTime: '',
            startAt: ''
          },
          stop: {
            inProgress: false
          },
          disableChat: {
            inProgress: false
          },
          enableChat: {
            inProgress: false
          },
          selection: {
            filter: '',
            fields: [
              { key: 'name', label: 'Name' },
              { key: 'desc', label: 'Description'},
              { key: 'duration', label: 'Duration'}
            ],
            items: [],
            selected: null,
            inProgress: false
          }
        },
        selectionFilter: ''
      }
    },
    computed: {
      ...mapState('stream', [
        'status',
        'playlist',
        'mediaName',
        'mediaDuration',
        'startTime',
        'endTime',
        'seekTime',
        'live'
      ]),
      ...mapState('apiKey', ['apiKey']),
      ...mapState('chat', ['enabled']),
      ...mapGetters('time', ['now']),
      needApiKey() {
        return this.apiKey == null || this.apiKey.length === 0;
      },
      streamStopped() {
        // stream explicitly offline or "online but finished"
        return this.status === "OFFLINE"
            || (this.status === "ONLINE" && (this.now - this.endTime) >= 0);
      },
      streamStartingSoon() {
        // stream is online and not yet started
        return this.status === "ONLINE"
          && (this.startTime - this.now) > 0;
      },
      streamRunning() {
        // stream explicitly online and started and not yet finished
        return this.status === "ONLINE"
          && (this.now - this.startTime) >= 0
          && (!this.endTime || ((this.endTime - this.now) > 0));
      },
      streamPaused() {
        // stream explicitly paused
        return this.status === "PAUSE";
      }
    },
    watch: {
      selectionFilter(value) {
        this.form.selection.filter = value
        this.$refs.streamEntries.refresh()
      }
    },
    methods: {
      alwaysTrue() {
        return true;
      },
      disableChat() {
        this.form.disableChat.inProgress = true;
        axios.post('/chat/disable', {
          key: this.apiKey
        }).then(response => {
          this.showResult(true, response.data);
          this.form.disableChat.inProgress = false;
        }).catch(error => {
          if (error.response) {
            this.showResult(false, error.response.data);
          } else {
            this.showResult(false, error.message);
          }
          this.form.disableChat.inProgress = false;
        })
      },
      enableChat() {
        this.form.enableChat.inProgress = true;
        axios.post('/chat/enable', {
          key: this.apiKey
        }).then(response => {
          this.showResult(true, response.data);
          this.form.enableChat.inProgress = false;
        }).catch(error => {
          if (error.response) {
            this.showResult(false, error.response.data);
          } else {
            this.showResult(false, error.message);
          }
          this.form.enableChat.inProgress = false;
        })
      },
      resetApiKey() {
        this.form.apiKey = this.apiKey; // populate the old value into the field
        this.$store.dispatch('apiKey/set', ''); // clear the api key
      },
      startStream() {
        this.form.start.inProgress = true;
        axios.post('/stream/start', {
          key: this.apiKey,
          name: this.form.start.name,
          seekTime: this.convertSeekTime(this.form.start.seekTime),
          delay: this.convertDelay(this.form.start.delay),
          startAt: this.convertStartAt(this.form.start.startAt)
        }).then(response => {
          this.showResult(true, response.data);
          this.form.start.inProgress = false;
        }).catch(error => {
          if (error.response) {
            this.showResult(false, error.response.data);
          } else {
            this.showResult(false, error.message);
          }
          this.form.start.inProgress = false;
        })
      },
      pauseStream() {
        this.form.pause.inProgress = true;
        axios.post('/stream/pause', {
          key: this.apiKey
        }).then(response => {
          this.showResult(true, response.data);
          this.form.pause.inProgress = false;
        }).catch(error => {
          if (error.response) {
            this.showResult(false, error.response.data);
          } else {
            this.showResult(false, error.message);
          }
          this.form.pause.inProgress = false;
        })
      },
      resumeStream() {
        this.form.resume.inProgress = true;
        axios.post('/stream/resume', {
          key: this.apiKey,
          delay: this.convertDelay(this.form.resume.delay),
          seekTime: this.convertSeekTime(this.form.resume.seekTime),
          startAt: this.convertStartAt(this.form.resume.startAt)
        }).then(response => {
          this.showResult(true, response.data);
          this.form.resume.inProgress = false;
        }).catch(error => {
          if (error.response) {
            this.showResult(false, error.response.data);
          } else {
            this.showResult(false, error.message);
          }
          this.form.resume.inProgress = false;
        })
      },
      stopStream() {
        this.form.stop.inProgress = true;
        axios.post('/stream/stop', {
          key: this.apiKey
        }).then(response => {
          this.showResult(true, response.data);
          this.form.stop.inProgress = false;
        }).catch(error => {
          if (error.response) {
            this.showResult(false, error.response.data);
          } else {
            this.showResult(false, error.message);
          }
          this.form.stop.inProgress = false;
        })
      },
      onRowSelected(items) {
        this.form.selection.selected = items;
        if (items.length == 1) {
          let entry = items[0];
          [ this.form.start.name ] = [ entry.name ]
        }
      },
      provideStreamEntries(ctx) {
        return axios.post('/stream/dir', {
          key: this.apiKey,
          filter: this.form.selection.filter
        }).then(response => {
          const entries = response.data.entries;
          return(entries)
        }).catch(error => {
          if (error.response) {
            this.showResult(false, error.response.data);
          } else {
            this.showResult(false, error.message);
          }
          return([])
        })
      },
      showResult(successValue, messageValue) {
        this.$emit("showResult", {
          message: messageValue,
          success: successValue
        });
      },
      convertDelay(delayTimeStr) {
        if (delayTimeStr == null || delayTimeStr.length === 0) {
          return null;
        }
        return parseInt(delayTimeStr) * 1000;
      },
      convertSeekTime(seekTimeStr) {
        if (seekTimeStr == null || seekTimeStr.length === 0) {
          return null;
        }
        let seconds = 0;
        let minutes = 1;
        let tokens = seekTimeStr.split(':');
        while (tokens.length > 0) {
          seconds += minutes * parseInt(tokens.pop());
          minutes *= 60;
        }
        return seconds * 1000;
      },
      convertStartAt(startAtStr) {
        if (startAtStr == null || startAtStr.length === 0) {
          return null;
        }
        let dt = DateTime.fromFormat(startAtStr, ["h:mm a"]);
        if (dt < DateTime.now()) {
          dt.add(1, 'days');
        }
        return dt.toMillis();
      },
      liveOrDuration(metadata) {
        return metadata.live ? "LIVE" : videojs.formatTime(metadata.duration / 1000, 1)
      }
    }
  }
</script>

