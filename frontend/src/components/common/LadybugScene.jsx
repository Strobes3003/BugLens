import { useEffect, useRef } from 'react'
import * as THREE from 'three'
import './LadybugScene.css'

const RED = 0xa3172c
const RED_DEEP = 0x7c0f1f
const BLACK = 0x1b1417
const BG = 0xfffaf8

// Builds the ladybug as a THREE.Group. Head faces -Z so lookAt() orients
// her correctly toward wherever she's flying.
function buildLadybug() {
  const bug = new THREE.Group()

  const shellMat = new THREE.MeshStandardMaterial({
    color: RED,
    roughness: 0.28,
    metalness: 0.18,
  })
  const bellyMat = new THREE.MeshStandardMaterial({
    color: RED_DEEP,
    roughness: 0.6,
    metalness: 0.05,
  })
  const blackMat = new THREE.MeshStandardMaterial({
    color: BLACK,
    roughness: 0.4,
    metalness: 0.2,
  })
  const shinyBlack = new THREE.MeshStandardMaterial({
    color: BLACK,
    roughness: 0.1,
    metalness: 0.5,
  })
  const eyeMat = new THREE.MeshBasicMaterial({ color: 0xffffff })

  // Belly (dark underside)
  const belly = new THREE.Mesh(new THREE.SphereGeometry(1, 32, 24), bellyMat)
  belly.scale.set(0.92, 0.38, 1.05)
  belly.position.y = -0.15
  belly.castShadow = true
  bug.add(belly)

  // Red shell dome
  const shell = new THREE.Mesh(new THREE.SphereGeometry(1, 48, 32), shellMat)
  shell.scale.set(0.92, 0.62, 1.05)
  shell.castShadow = true
  bug.add(shell)

  // Center seam
  const seam = new THREE.Mesh(
    new THREE.BoxGeometry(0.025, 0.05, 2.02),
    blackMat,
  )
  seam.position.y = 0.6
  bug.add(seam)

  // 7-spot arrangement (Coccinella septempunctata)
  const spotGeo = new THREE.SphereGeometry(0.14, 16, 12)
  const spotPositions = [
    [0, 0.6, -0.3],
    [0.42, 0.55, -0.5],
    [-0.42, 0.55, -0.5],
    [0.5, 0.52, 0.05],
    [-0.5, 0.52, 0.05],
    [0.4, 0.52, 0.55],
    [-0.4, 0.52, 0.55],
  ]
  spotPositions.forEach(([x, y, z]) => {
    const s = new THREE.Mesh(spotGeo, blackMat)
    s.position.set(x, y, z)
    s.scale.set(1, 0.4, 1)
    s.castShadow = true
    bug.add(s)
  })

  // Head
  const head = new THREE.Mesh(
    new THREE.SphereGeometry(0.42, 32, 24),
    shinyBlack,
  )
  head.position.set(0, 0.12, -0.9)
  head.scale.set(1.15, 0.85, 0.9)
  head.castShadow = true
  bug.add(head)

  // Eyes (white sphere + small black pupil)
  ;[-1, 1].forEach((side) => {
    const eye = new THREE.Mesh(new THREE.SphereGeometry(0.09, 16, 12), eyeMat)
    eye.position.set(0.22 * side, 0.25, -1.18)
    bug.add(eye)
    const pupil = new THREE.Mesh(
      new THREE.SphereGeometry(0.045, 12, 10),
      blackMat,
    )
    pupil.position.set(0.22 * side, 0.25, -1.26)
    bug.add(pupil)
  })

  // Antennae
  ;[-1, 1].forEach((side) => {
    const stemGeo = new THREE.CylinderGeometry(0.018, 0.012, 0.65, 8)
    const stem = new THREE.Mesh(stemGeo, blackMat)
    stem.position.set(0.16 * side, 0.5, -1.0)
    stem.rotation.z = side * 0.5
    stem.rotation.x = -0.6
    bug.add(stem)
    const tip = new THREE.Mesh(new THREE.SphereGeometry(0.07, 12, 10), blackMat)
    tip.position.set(0.38 * side, 0.78, -1.28)
    bug.add(tip)
  })

  // Legs (6)
  const legGeo = new THREE.CylinderGeometry(0.035, 0.022, 0.55, 8)
  const legConfig = [
    [0.55, -0.15, -0.55, 0.9],
    [-0.55, -0.15, -0.55, -0.9],
    [0.7, -0.15, 0.0, 1.05],
    [-0.7, -0.15, 0.0, -1.05],
    [0.55, -0.15, 0.55, 0.9],
    [-0.55, -0.15, 0.55, -0.9],
  ]
  legConfig.forEach(([x, y, z, zRot]) => {
    const leg = new THREE.Mesh(legGeo, blackMat)
    leg.position.set(x, y, z)
    leg.rotation.z = zRot
    leg.castShadow = true
    bug.add(leg)
  })

  // Wings — translucent membranes on hinged pivots
  const wingShape = new THREE.Shape()
  wingShape.moveTo(0, 0)
  wingShape.bezierCurveTo(0.4, 0.55, 1.6, 0.5, 2.0, 0.1)
  wingShape.bezierCurveTo(2.15, -0.35, 1.4, -0.9, 0.4, -0.5)
  wingShape.bezierCurveTo(0.15, -0.35, 0.05, -0.15, 0, 0)

  const wingGeo = new THREE.ShapeGeometry(wingShape, 32)
  const wingMat = new THREE.MeshStandardMaterial({
    color: 0xfff5f0,
    transparent: true,
    opacity: 0.45,
    side: THREE.DoubleSide,
    roughness: 0.15,
    metalness: 0.05,
  })

  const wings = [1, -1].map((side) => {
    const pivot = new THREE.Group()
    pivot.position.set(0.05 * side, 0.35, 0.15)
    const mesh = new THREE.Mesh(wingGeo, wingMat)
    mesh.rotation.x = -Math.PI / 2
    mesh.scale.x = side
    pivot.add(mesh)
    bug.add(pivot)
    return pivot
  })

  bug.userData.wings = wings
  return bug
}

// Floating dust motes for atmosphere
function buildDust(count = 80) {
  const geo = new THREE.BufferGeometry()
  const positions = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 20
    positions[i * 3 + 1] = (Math.random() - 0.5) * 10
    positions[i * 3 + 2] = (Math.random() - 0.5) * 6
  }
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const mat = new THREE.PointsMaterial({
    color: RED,
    size: 0.05,
    transparent: true,
    opacity: 0.35,
    sizeAttenuation: true,
  })
  return new THREE.Points(geo, mat)
}

export default function LadybugScene() {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    // --- Scene / camera / renderer ---
    const scene = new THREE.Scene()
    scene.background = new THREE.Color(BG)
    scene.fog = new THREE.Fog(BG, 18, 32)

    const camera = new THREE.PerspectiveCamera(
      42,
      window.innerWidth / window.innerHeight,
      0.1,
      100,
    )
    camera.position.set(0, 0.5, 12)

    const renderer = new THREE.WebGLRenderer({ canvas, antialias: true })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setSize(window.innerWidth, window.innerHeight)
    renderer.shadowMap.enabled = true
    renderer.shadowMap.type = THREE.PCFSoftShadowMap

    // --- Lights ---
    scene.add(new THREE.AmbientLight(0xffffff, 0.55))
    const key = new THREE.DirectionalLight(0xffffff, 1.1)
    key.position.set(5, 8, 6)
    key.castShadow = true
    key.shadow.mapSize.set(2048, 2048)
    key.shadow.camera.left = -8
    key.shadow.camera.right = 8
    key.shadow.camera.top = 8
    key.shadow.camera.bottom = -8
    key.shadow.bias = -0.0005
    scene.add(key)
    const fill = new THREE.DirectionalLight(0xffd8c8, 0.35)
    fill.position.set(-6, 2, -4)
    scene.add(fill)
    const rim = new THREE.DirectionalLight(0xff9080, 0.25)
    rim.position.set(0, -5, -5)
    scene.add(rim)

    // Soft ground shadow catcher
    const ground = new THREE.Mesh(
      new THREE.PlaneGeometry(60, 60),
      new THREE.ShadowMaterial({ opacity: 0.18 }),
    )
    ground.rotation.x = -Math.PI / 2
    ground.position.y = -4.5
    ground.receiveShadow = true
    scene.add(ground)

    const bug = buildLadybug()
    scene.add(bug)

    const dust = buildDust()
    scene.add(dust)

    // --- Interaction state ---
    const mouseNDC = new THREE.Vector2(0, 0)
    const cursorWorld = new THREE.Vector3()
    const raycaster = new THREE.Raycaster()
    const hitPlane = new THREE.Plane(new THREE.Vector3(0, 0, 1), 0)
    const ambientTarget = new THREE.Vector3()
    const currentTarget = new THREE.Vector3()
    const velocity = new THREE.Vector3()
    const prevPosition = new THREE.Vector3()
    let mouseIdleTime = 999
    let bugHovered = false
    let loopTime = 0
    let loopSpin = 0
    let nextAmbientChange = 0

    const onMouseMove = (e) => {
      mouseNDC.x = (e.clientX / window.innerWidth) * 2 - 1
      mouseNDC.y = -(e.clientY / window.innerHeight) * 2 + 1
      mouseIdleTime = 0
      raycaster.setFromCamera(mouseNDC, camera)
      raycaster.ray.intersectPlane(hitPlane, cursorWorld)
    }
    const triggerCelebration = () => {
      loopTime = 1.2
      loopSpin = 0
    }
    const onClick = (e) => {
      if (e.target.closest('.auth-card')) return
      triggerCelebration()
    }
    const onResize = () => {
      camera.aspect = window.innerWidth / window.innerHeight
      camera.updateProjectionMatrix()
      renderer.setSize(window.innerWidth, window.innerHeight)
    }

    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('click', onClick)
    window.addEventListener('resize', onResize)
    // Any code (e.g. LoginForm on submit) can trigger the celebration
    // via: window.dispatchEvent(new Event('buglens:celebrate'))
    window.addEventListener('buglens:celebrate', triggerCelebration)

    // --- Animation loop ---
    const clock = new THREE.Clock()
    let animationId = 0

    const animate = () => {
      animationId = requestAnimationFrame(animate)
      const dt = Math.min(clock.getDelta(), 0.05)
      const t = clock.getElapsedTime()

      mouseIdleTime += dt

      // Pick a fresh ambient wander point every few seconds
      if (t > nextAmbientChange) {
        ambientTarget.set(
          (Math.random() - 0.5) * 8,
          (Math.random() - 0.5) * 4,
          (Math.random() - 0.5) * 3,
        )
        nextAmbientChange = t + 2.5 + Math.random() * 2
      }

      const followingCursor = mouseIdleTime < 2.0
      if (followingCursor) {
        currentTarget.lerp(cursorWorld, 0.15)
      } else {
        currentTarget.lerp(ambientTarget, 0.02)
      }

      raycaster.setFromCamera(mouseNDC, camera)
      bugHovered = raycaster.intersectObject(bug, true).length > 0
      prevPosition.copy(bug.position)

      if (loopTime > 0) {
        // Celebration: loop-de-loop around current position
        loopTime -= dt
        loopSpin += dt * 8
        const centerX = bug.position.x
        const centerY = bug.position.y + 1.2
        bug.position.x = centerX + Math.cos(loopSpin - Math.PI / 2) * 0.45
        bug.position.y = centerY + Math.sin(loopSpin - Math.PI / 2) * 1.5
        bug.rotation.x = loopSpin
      } else {
        const lerpSpeed = followingCursor ? 3.5 : 1.2
        bug.position.lerp(currentTarget, Math.min(dt * lerpSpeed, 1))
        bug.position.y += Math.sin(t * 6) * 0.008

        velocity.subVectors(bug.position, prevPosition)
        if (velocity.length() > 0.005) {
          const lookTarget = new THREE.Vector3().addVectors(
            bug.position,
            velocity.clone().multiplyScalar(30),
          )
          const tempMat = new THREE.Matrix4().lookAt(
            bug.position,
            lookTarget,
            new THREE.Vector3(0, 1, 0),
          )
          const tempQuat = new THREE.Quaternion().setFromRotationMatrix(tempMat)
          bug.quaternion.slerp(tempQuat, 0.15)

          // Bank into turns
          const turnBank = -velocity.x * 3
          bug.rotation.z = THREE.MathUtils.lerp(
            bug.rotation.z,
            THREE.MathUtils.clamp(turnBank, -0.6, 0.6),
            0.1,
          )
        }
      }

      // Scale pulse when the cursor is directly on her
      const targetScale = bugHovered ? 1.15 : 1.0
      const s = THREE.MathUtils.lerp(bug.scale.x, targetScale, 0.15)
      bug.scale.set(s, s, s)

      // Wing flap — speed changes with mood
      const flapSpeed =
        loopTime > 0 ? 80 : bugHovered ? 65 : followingCursor ? 50 : 38
      const flapAmount = 0.6 + Math.sin(t * flapSpeed) * 0.7
      bug.userData.wings[0].rotation.z = flapAmount
      bug.userData.wings[1].rotation.z = -flapAmount
      bug.userData.wings[0].rotation.y =
        -0.2 + Math.sin(t * flapSpeed) * 0.15
      bug.userData.wings[1].rotation.y =
        0.2 - Math.sin(t * flapSpeed) * 0.15

      // Drift dust motes up and wrap
      const dustPos = dust.geometry.attributes.position.array
      for (let i = 0; i < dustPos.length / 3; i++) {
        dustPos[i * 3 + 1] += 0.005 + Math.sin(t + i) * 0.002
        if (dustPos[i * 3 + 1] > 6) dustPos[i * 3 + 1] = -6
      }
      dust.geometry.attributes.position.needsUpdate = true

      renderer.render(scene, camera)
    }
    animate()

    // --- Cleanup ---
    return () => {
      cancelAnimationFrame(animationId)
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('click', onClick)
      window.removeEventListener('resize', onResize)
      window.removeEventListener('buglens:celebrate', triggerCelebration)
      scene.traverse((obj) => {
        if (obj.geometry) obj.geometry.dispose()
        if (obj.material) {
          if (Array.isArray(obj.material)) {
            obj.material.forEach((m) => m.dispose())
          } else {
            obj.material.dispose()
          }
        }
      })
      renderer.dispose()
    }
  }, [])

  return (
    <canvas ref={canvasRef} className="ladybug-scene" aria-hidden="true" />
  )
}
