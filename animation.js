// 设置视频图片路径和总帧数
const imagePrefix = 'frames_obama/code_swarm-';
const imageSuffix = '.png';
const totalFrames = 28388;

// 初始化播放暂停按钮和进度条
const playPauseButton = document.getElementById('play-pause-button');
const progressBar = document.getElementById('progress-bar');
progressBar.style.width = '50%';
const intervalInput = document.getElementById('interval-input');
const updateIntervalBtn = document.getElementById('update-interval-button');
let timer;
let interval = 50;

updateIntervalBtn.addEventListener('click', function() {
  interval = parseInt(intervalInput.value);
  var text = document.getElementById("update-message");
  text.innerHTML = "Interval updated to " + interval + "ms";
  text.style.display = "block";
  setTimeout(function() {
    text.style.display = "none";
  }
  // 3秒后消失
  , 3000);

  clearInterval(timer)
  // 播放循环
  timer = setInterval(() => {
    if (isPlaying) {
      currentFrame += 1;
      if (currentFrame > totalFrames) {
        currentFrame = 1;
      }
      updateFrame();
    }
  }, interval);
});


// 初始化播放状态和当前帧数
let isPlaying = false;
let currentFrame = 1;

// 切换播放状态
function togglePlay() {
  if (isPlaying) {
    pause();
  } else {
    play();
  }
}

// 播放
function play() {
  isPlaying = true;
  playPauseButton.innerText = 'Pause';
  updateFrame();
}

// 暂停
function pause() {
  isPlaying = false;
  playPauseButton.innerText = 'Play';
}

// 更新当前帧图片
function updateFrame() {
  const image = document.getElementById('video-frame');
  image.src = imagePrefix + currentFrame.toString().padStart(5, '0') + imageSuffix;
}

// 切换帧数
function updateProgress() {
  const newFrame = Math.round(progressBar.value / 100 * totalFrames);
  currentFrame = newFrame;
  updateFrame();
}

// 绑定事件
playPauseButton.addEventListener('click', togglePlay);
progressBar.addEventListener('input', updateProgress);



